package com.pruebatecnica.polizas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.pruebatecnica.polizas.core.CoreClient;
import com.pruebatecnica.polizas.domain.Poliza;
import com.pruebatecnica.polizas.domain.Riesgo;
import com.pruebatecnica.polizas.repository.PolizaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = {"polizas.datos-iniciales=false", "ipc.sincronizacion.habilitada=false"})
class ConcurrenciaTest {

    private static final int RONDAS = 30;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PolizaRepository polizas;

    @MockitoBean
    private CoreClient core;

    @Test
    void dosCancelacionesSimultaneasNuncaDejanLaColectivaSinRiesgos() throws Exception {
        try (ExecutorService hilos = Executors.newFixedThreadPool(2)) {
            for (int ronda = 0; ronda < RONDAS; ronda++) {
                Poliza poliza = Poliza.colectiva("I", "P", LocalDate.of(2026, 1, 1), 6, new BigDecimal("1000"), "A", "D1");
                poliza.agregarRiesgo("B", "D2");
                polizas.save(poliza);

                CyclicBarrier salida = new CyclicBarrier(2);
                List<Future<Integer>> respuestas = new ArrayList<>();
                for (Riesgo riesgo : poliza.getRiesgos()) {
                    Long id = riesgo.getId();
                    respuestas.add(hilos.submit(() -> {
                        salida.await();
                        return mvc.perform(post("/riesgos/{id}/cancelar", id).header("x-api-key", "123456"))
                                .andReturn().getResponse().getStatus();
                    }));
                }

                assertThat(List.of(respuestas.get(0).get(), respuestas.get(1).get()))
                        .containsExactlyInAnyOrder(200, 409);
            }
        }
    }
}
