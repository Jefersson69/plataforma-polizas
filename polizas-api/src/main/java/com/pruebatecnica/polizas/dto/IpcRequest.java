package com.pruebatecnica.polizas.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record IpcRequest(
        @NotNull(message = "es obligatorio")
        @DecimalMin(value = "0", message = "debe ser mayor o igual a 0")
        @DecimalMax(value = "100", message = "debe ser menor o igual a 100")
        BigDecimal porcentaje) {
}
