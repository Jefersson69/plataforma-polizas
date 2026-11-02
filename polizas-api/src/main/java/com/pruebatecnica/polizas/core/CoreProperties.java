package com.pruebatecnica.polizas.core;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("core")
public record CoreProperties(@NotBlank String baseUrl, @NotBlank String apiKey) {
}
