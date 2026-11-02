package com.pruebatecnica.polizas.ipc;

import java.math.BigDecimal;
import java.util.Map;

/** Puerto hacia la fuente del IPC. Si no hay dato lanza excepcion, nunca lo supone. */
public interface IpcProvider {

    String nombre();

    Map<Integer, BigDecimal> obtenerVariacionesAnuales();
}
