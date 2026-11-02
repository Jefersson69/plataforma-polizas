package com.pruebatecnica.polizas.dto;

import com.pruebatecnica.polizas.domain.IpcAnual;
import java.math.BigDecimal;
import java.time.LocalDate;

public record IpcResponse(Integer anio, BigDecimal porcentaje, String fuente, LocalDate fechaRegistro) {

    public static IpcResponse desde(IpcAnual ipc) {
        return new IpcResponse(ipc.getAnio(), ipc.getPorcentaje(), ipc.getFuente(), ipc.getFechaRegistro());
    }
}
