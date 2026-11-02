package com.pruebatecnica.polizas.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pruebatecnica.polizas.core.CoreClient;
import com.pruebatecnica.polizas.domain.EstadoPoliza;
import com.pruebatecnica.polizas.domain.Poliza;
import com.pruebatecnica.polizas.repository.PolizaRepository;
import com.pruebatecnica.polizas.repository.RiesgoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** En local se puede renovar a la primera y sin internet. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:datosdemo")
class DatosInicialesTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CoreClient core;

    @Autowired
    private PolizaRepository polizas;

    @Autowired
    private RiesgoRepository riesgos;

    @Test
    void cargaLasPolizasDelArchivo() {
        assertThat(polizas.findAll()).hasSize(10).extracting(Poliza::getEstado).contains(EstadoPoliza.CANCELADA);
        assertThat(riesgos.count()).isEqualTo(15);
    }

    @Test
    void permiteRenovarConLosDatosDeEjemplo() throws Exception {
        mvc.perform(get("/ipc").header("x-api-key", "123456"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fuente").value("EJEMPLO"));

        mvc.perform(post("/polizas/1/renovar").header("x-api-key", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canon").value(1576500.00));
    }
}
