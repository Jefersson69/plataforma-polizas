package com.pruebatecnica.polizas.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

/** Prueba el adaptador real con un servidor local, porque en las demas pruebas el CORE se simula. */
class HttpCoreClientTest {

    private HttpServer servidor;
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private final AtomicReference<String> clave = new AtomicReference<>();

    @BeforeEach
    void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.start();
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    private HttpCoreClient cliente() {
        return new HttpCoreClient(new CoreProperties("http://localhost:" + servidor.getAddress().getPort(), "clave-core"));
    }

    private void responder(int estado) {
        servidor.createContext("/core-mock/evento", intercambio -> {
            cuerpo.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            clave.set(intercambio.getRequestHeaders().getFirst("x-api-key"));
            intercambio.sendResponseHeaders(estado, -1);
            intercambio.close();
        });
    }

    @Test
    void enviaElEventoConElContratoEsperadoYSuPropiaClave() {
        responder(202);

        cliente().notificarActualizacion(555L);

        assertThat(cuerpo.get()).contains("\"evento\":\"ACTUALIZACION\"").contains("\"polizaId\":555");
        assertThat(clave.get()).isEqualTo("clave-core");
    }

    @Test
    void unaRespuestaDeErrorSePropaga() {
        responder(500);

        assertThatThrownBy(() -> cliente().notificarActualizacion(1L)).isInstanceOf(RestClientException.class);
    }

    @Test
    void unServidorLentoSeCortaPorTimeout() {
        servidor.createContext("/core-mock/evento", intercambio -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            intercambio.close();
        });

        long inicio = System.currentTimeMillis();
        assertThatThrownBy(() -> cliente().notificarActualizacion(1L)).isInstanceOf(RestClientException.class);
        assertThat(System.currentTimeMillis() - inicio).isLessThan(4000);
    }
}
