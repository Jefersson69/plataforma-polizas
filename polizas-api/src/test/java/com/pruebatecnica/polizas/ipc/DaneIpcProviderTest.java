package com.pruebatecnica.polizas.ipc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Indices de diciembre del DANE; 5,20 y 5,10 son las variaciones de 2024 y 2025. */
class DaneIpcProviderTest {

    private static final String RUTA_EXCEL = "/files/operaciones/IPC/sep2026/anex-IPC-Indices-sep2026.xlsx";

    private HttpServer servidor;

    @BeforeEach
    void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.start();
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    private DaneIpcProvider proveedor() {
        return new DaneIpcProvider(new DaneProperties("http://localhost:" + servidor.getAddress().getPort() + "/ipc"));
    }

    private void publicar(String ruta, byte[] contenido) {
        servidor.createContext(ruta, intercambio -> {
            intercambio.sendResponseHeaders(200, contenido.length);
            intercambio.getResponseBody().write(contenido);
            intercambio.close();
        });
    }

    private static byte[] excel(String tituloMes) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("IndicesIPC");
            hoja.createRow(0).createCell(0).setCellValue("Número Índice");
            Row encabezado = hoja.createRow(1);
            encabezado.createCell(0).setCellValue("Mes");
            Row diciembre = hoja.createRow(2);
            diciembre.createCell(0).setCellValue(tituloMes);
            double[][] datos = {{2023, 137.72}, {2024, 144.88}, {2025, 152.27}, {2026, 0}};
            for (int i = 0; i < datos.length; i++) {
                encabezado.createCell(i + 1).setCellValue(datos[i][0]);
                if (datos[i][1] > 0) {
                    diciembre.createCell(i + 1).setCellValue(datos[i][1]);
                }
            }
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private static byte[] pagina(String enlace) {
        return ("<html><a href=\"" + enlace + "\">Anexo</a></html>").getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void calculaLaVariacionDiciembreContraDiciembreYOmiteAnosSinDato() throws IOException {
        publicar("/ipc", pagina(RUTA_EXCEL));
        publicar(RUTA_EXCEL, excel("Diciembre"));

        Map<Integer, BigDecimal> variaciones = proveedor().obtenerVariacionesAnuales();

        assertThat(variaciones).containsOnlyKeys(2024, 2025);
        assertThat(variaciones.get(2024)).isEqualByComparingTo("5.20");
        assertThat(variaciones.get(2025)).isEqualByComparingTo("5.10");
    }

    @Test
    void fallaSiLaPaginaNoTieneElEnlace() {
        publicar("/ipc", "<html>sin anexos</html>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> proveedor().obtenerVariacionesAnuales())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("anexo");
    }

    @Test
    void fallaSiElArchivoCambiaDeFormato() throws IOException {
        publicar("/ipc", pagina(RUTA_EXCEL));
        publicar(RUTA_EXCEL, excel("Dic"));

        assertThatThrownBy(() -> proveedor().obtenerVariacionesAnuales())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("formato");
    }

    @Test
    void fallaSiElArchivoNoEsUnExcelValido() {
        publicar("/ipc", pagina(RUTA_EXCEL));
        publicar(RUTA_EXCEL, "no soy un excel".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> proveedor().obtenerVariacionesAnuales()).isInstanceOf(RuntimeException.class);
    }
}
