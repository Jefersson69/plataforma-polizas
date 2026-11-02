package com.pruebatecnica.polizas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.pruebatecnica.polizas.core.CoreClient;
import com.pruebatecnica.polizas.domain.Poliza;
import com.pruebatecnica.polizas.repository.PolizaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sin conexiones libres responde 503. Tambien deja documentado que la conexion
 * sigue ocupada mientras se avisa al CORE.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = {"polizas.datos-iniciales=false", "ipc.sincronizacion.habilitada=false",
        "spring.datasource.url=jdbc:h2:mem:saturacion", "spring.datasource.hikari.maximum-pool-size=1",
        "spring.datasource.hikari.connection-timeout=500"})
class SaturacionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PolizaRepository polizas;

    @MockitoBean
    private CoreClient core;

    @Test
    void sinConexionesLibresResponde503() throws Exception {
        Poliza poliza = polizas.save(Poliza.individual("A", "B", "C", LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000")));
        CountDownLatch dentro = new CountDownLatch(1);
        CountDownLatch soltar = new CountDownLatch(1);
        doAnswer(i -> {
            dentro.countDown();
            soltar.await(10, TimeUnit.SECONDS);
            return null;
        }).when(core).notificarActualizacion(any());

        Thread escritura = new Thread(() -> {
            try {
                mvc.perform(post("/polizas/{id}/cancelar", poliza.getId()).header("x-api-key", "123456"));
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
        escritura.start();
        assertThat(dentro.await(5, TimeUnit.SECONDS)).isTrue();

        int estado = mvc.perform(get("/polizas").header("x-api-key", "123456")).andReturn().getResponse().getStatus();

        soltar.countDown();
        escritura.join();
        assertThat(estado).isEqualTo(503);
    }
}
