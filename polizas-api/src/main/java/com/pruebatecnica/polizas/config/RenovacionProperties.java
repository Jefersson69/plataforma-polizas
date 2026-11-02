package com.pruebatecnica.polizas.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("polizas.renovacion")
public record RenovacionProperties(@NotNull @Min(0) Integer ventanaDias) {
}
