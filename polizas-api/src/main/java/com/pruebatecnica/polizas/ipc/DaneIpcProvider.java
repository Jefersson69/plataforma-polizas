package com.pruebatecnica.polizas.ipc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * El DANE no tiene API, publica un Excel cada mes y el nombre cambia.
 * Por eso se busca el enlace en su pagina y se descarga de ahi.
 */
@Component
class DaneIpcProvider implements IpcProvider {

    private static final Pattern ENLACE_INDICES =
            Pattern.compile("href=\"(/files/operaciones/IPC/[^\"]*anex-IPC-Indices-[^\"]*\\.xlsx)\"");
    private static final int TAMANO_MAXIMO = 5 * 1024 * 1024;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final URI pagina;
    private final RestClient restClient;

    DaneIpcProvider(DaneProperties propiedades) {
        this.pagina = URI.create(propiedades.paginaUrl());
        HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(TIMEOUT);
        this.restClient = RestClient.builder()
                .requestFactory(fabrica)
                .defaultHeader("User-Agent", "polizas-api")
                .build();
    }

    @Override
    public String nombre() {
        return "DANE";
    }

    @Override
    public Map<Integer, BigDecimal> obtenerVariacionesAnuales() {
        String html = restClient.get().uri(pagina).retrieve().body(String.class);
        Matcher enlace = ENLACE_INDICES.matcher(html == null ? "" : html);
        if (!enlace.find()) {
            throw new IllegalStateException("La pagina del DANE no contiene el anexo de indices");
        }
        byte[] archivo = descargar(pagina.resolve(enlace.group(1)));
        try {
            return DaneIpcParser.leerVariaciones(new ByteArrayInputStream(archivo));
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el archivo del DANE", ex);
        }
    }

    private byte[] descargar(URI archivo) {
        return restClient.get().uri(archivo).exchange((peticion, respuesta) -> {
            if (!respuesta.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("El DANE respondio " + respuesta.getStatusCode());
            }
            byte[] contenido = respuesta.getBody().readNBytes(TAMANO_MAXIMO + 1);
            if (contenido.length > TAMANO_MAXIMO) {
                throw new IllegalStateException("El archivo del DANE supera el tamano permitido");
            }
            return contenido;
        });
    }
}
