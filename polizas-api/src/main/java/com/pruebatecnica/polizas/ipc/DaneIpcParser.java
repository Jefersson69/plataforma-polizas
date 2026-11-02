package com.pruebatecnica.polizas.ipc;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Lee el Excel de indices del DANE: filas por mes y columnas por ano.
 * La variacion es diciembre de ese ano contra diciembre del anterior.
 */
final class DaneIpcParser {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private DaneIpcParser() {
    }

    static Map<Integer, BigDecimal> leerVariaciones(InputStream archivo) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(archivo)) {
            Sheet hoja = libro.getSheetAt(0);
            Row encabezado = buscarFila(hoja, "Mes");
            Row diciembre = buscarFila(hoja, "Diciembre");

            Map<Integer, BigDecimal> indices = new HashMap<>();
            for (Cell celda : encabezado) {
                if (celda.getCellType() == CellType.NUMERIC) {
                    Cell valor = diciembre.getCell(celda.getColumnIndex());
                    if (valor != null && valor.getCellType() == CellType.NUMERIC) {
                        indices.put((int) celda.getNumericCellValue(),
                                BigDecimal.valueOf(valor.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP));
                    }
                }
            }

            Map<Integer, BigDecimal> variaciones = new TreeMap<>();
            indices.forEach((anio, indice) -> {
                BigDecimal anterior = indices.get(anio - 1);
                if (anterior != null) {
                    variaciones.put(anio, indice.multiply(CIEN).divide(anterior, 2, RoundingMode.HALF_UP).subtract(CIEN));
                }
            });
            if (variaciones.isEmpty()) {
                throw new IllegalStateException("El archivo del DANE no contiene variaciones anuales");
            }
            return variaciones;
        }
    }

    private static Row buscarFila(Sheet hoja, String titulo) {
        for (Row fila : hoja) {
            Cell primera = fila.getCell(0);
            if (primera != null && primera.getCellType() == CellType.STRING
                    && titulo.equals(primera.getStringCellValue().trim())) {
                return fila;
            }
        }
        throw new IllegalStateException("El archivo del DANE cambio de formato: no se encontro la fila " + titulo);
    }
}
