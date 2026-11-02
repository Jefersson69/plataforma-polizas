package com.pruebatecnica.polizas.ipc;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("ipc.dane")
public record DaneProperties(@NotBlank String paginaUrl) {
}
