package com.pruebatecnica.polizas;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

class ConfiguracionObligatoriaTest {

    private static final String SIN_SINCRONIZAR = "--ipc.sincronizacion.habilitada=false";
    private static final String BASE_MEMORIA = "--spring.datasource.url=jdbc:h2:mem:sinclave";

    private static void arrancar(String... argumentos) {
        new SpringApplicationBuilder(PolizasApiApplication.class).web(WebApplicationType.NONE).run(argumentos);
    }

    @Test
    void sinConfiguracionDelCoreLaAplicacionNoArranca() {
        assertThatThrownBy(() -> arrancar(SIN_SINCRONIZAR, BASE_MEMORIA, "--seguridad.api-key=clave"))
                .hasStackTraceContaining("CoreProperties");
    }

    @Test
    void sinClaveDeLaApiLaAplicacionNoArranca() {
        assertThatThrownBy(() -> arrancar(SIN_SINCRONIZAR, BASE_MEMORIA,
                "--core.api-key=clave", "--core.base-url=http://localhost:9"))
                .hasStackTraceContaining("SeguridadProperties");
    }
}
