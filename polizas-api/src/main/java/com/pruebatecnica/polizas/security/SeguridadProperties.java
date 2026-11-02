package com.pruebatecnica.polizas.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Sin clave no arranca, asi nunca queda una clave conocida por defecto. */
@Validated
@ConfigurationProperties("seguridad")
public record SeguridadProperties(@NotBlank String apiKey) {
}
