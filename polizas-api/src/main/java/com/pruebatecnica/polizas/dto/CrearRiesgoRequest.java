package com.pruebatecnica.polizas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearRiesgoRequest(
        @NotBlank(message = "es obligatorio") @Size(max = 255, message = "admite hasta 255 caracteres") String arrendatario,
        @NotBlank(message = "es obligatorio") @Size(max = 255, message = "admite hasta 255 caracteres") String direccionInmueble) {
}
