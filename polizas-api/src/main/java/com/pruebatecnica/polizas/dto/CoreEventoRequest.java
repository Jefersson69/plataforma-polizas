package com.pruebatecnica.polizas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** El patron evita que metan saltos de linea para falsear los logs. */
public record CoreEventoRequest(
        @NotNull @Pattern(regexp = "[A-Z_]{1,50}") String evento,
        @NotNull Long polizaId) {
}
