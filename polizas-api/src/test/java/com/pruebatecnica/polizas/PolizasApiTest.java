package com.pruebatecnica.polizas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pruebatecnica.polizas.core.CoreClient;
import com.pruebatecnica.polizas.domain.EstadoPoliza;
import com.pruebatecnica.polizas.domain.IpcAnual;
import com.pruebatecnica.polizas.domain.Poliza;
import com.pruebatecnica.polizas.repository.IpcRepository;
import com.pruebatecnica.polizas.repository.PolizaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = {"polizas.datos-iniciales=false", "ipc.sincronizacion.habilitada=false"})
@ExtendWith(OutputCaptureExtension.class)
class PolizasApiTest {

    private static final String RIESGO_JSON = """
            {"arrendatario": "Luis", "direccionInmueble": "Apto 1"}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PolizaRepository polizas;

    @Autowired
    private IpcRepository ipcRepository;

    @Autowired
    private Clock reloj;

    @MockitoBean
    private CoreClient core;

    private LocalDate hoy;
    private LocalDate vencimiento;

    @BeforeEach
    void preparar() {
        polizas.deleteAll();
        ipcRepository.deleteAll();
        hoy = LocalDate.now(reloj);
        vencimiento = hoy.plusDays(30);
        ipcRepository.save(new IpcAnual(hoy.getYear() - 1, BigDecimal.TEN, "PRUEBA", hoy));
    }

    private static MockHttpServletRequestBuilder conClave(MockHttpServletRequestBuilder request) {
        return request.header("x-api-key", "123456");
    }

    private Poliza individual() {
        return polizas.save(Poliza.individual("Ana", "Carlos", "Calle 1", vencimiento.minusMonths(12), 12,
                new BigDecimal("1000")));
    }

    private Poliza colectiva(int riesgos) {
        Poliza poliza = Poliza.colectiva("Inmobiliaria", "Propietarios", vencimiento.minusMonths(6), 6,
                new BigDecimal("1000"), "Arrendatario 0", "Apto 0");
        for (int i = 1; i < riesgos; i++) {
            poliza.agregarRiesgo("Arrendatario " + i, "Apto " + i);
        }
        return polizas.save(poliza);
    }

    @Test
    void rechazaPeticionesSinClaveOConClaveIncorrecta() throws Exception {
        mvc.perform(get("/polizas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/polizas").header("x-api-key", "otra")).andExpect(status().isUnauthorized());
        mvc.perform(post("/core-mock/evento")).andExpect(status().isUnauthorized());
        mvc.perform(put("/ipc/2025")).andExpect(status().isUnauthorized());
    }

    @Test
    void listaFiltrandoPorTipoYEstado() throws Exception {
        Poliza individual = individual();
        Poliza colectiva = colectiva(1);

        mvc.perform(conClave(get("/polizas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(conClave(get("/polizas")).param("tipo", "COLECTIVA"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(colectiva.getId()));
        mvc.perform(conClave(get("/polizas")).param("tipo", "INDIVIDUAL").param("estado", "ACTIVA"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(individual.getId()));
        mvc.perform(conClave(get("/polizas")).param("estado", "CANCELADA"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unFiltroInvalidoDevuelve400() throws Exception {
        mvc.perform(conClave(get("/polizas")).param("tipo", "OTRO")).andExpect(status().isBadRequest());
    }

    @Test
    void listaLosRiesgosDeUnaPoliza() throws Exception {
        Poliza poliza = colectiva(2);

        mvc.perform(conClave(get("/polizas/{id}/riesgos", poliza.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].polizaId").value(poliza.getId()));
    }

    @Test
    void unaPolizaInexistenteDevuelve404() throws Exception {
        mvc.perform(conClave(get("/polizas/999999/riesgos"))).andExpect(status().isNotFound());
        mvc.perform(conClave(post("/polizas/999999/renovar"))).andExpect(status().isNotFound());
        mvc.perform(conClave(post("/riesgos/999999/cancelar"))).andExpect(status().isNotFound());
    }

    @Test
    void renovarAplicaElIpcDelAnoAnteriorYAvisaAlCore() throws Exception {
        Poliza poliza = individual();

        mvc.perform(conClave(post("/polizas/{id}/renovar", poliza.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RENOVADA"))
                .andExpect(jsonPath("$.canon").value(1100.00))
                .andExpect(jsonPath("$.prima").value(13200.00))
                .andExpect(jsonPath("$.fechaInicio").value(vencimiento.toString()))
                .andExpect(jsonPath("$.ipcAnioAplicado").value(hoy.getYear() - 1));

        verify(core).notificarActualizacion(poliza.getId());
    }

    @Test
    void renovarDosVecesDevuelve409YNoAplicaElIpcOtraVez() throws Exception {
        Poliza poliza = individual();
        mvc.perform(conClave(post("/polizas/{id}/renovar", poliza.getId()))).andExpect(status().isOk());

        mvc.perform(conClave(post("/polizas/{id}/renovar", poliza.getId()))).andExpect(status().isConflict());

        assertThat(polizas.findById(poliza.getId()).orElseThrow().getCanon()).isEqualByComparingTo("1100");
    }

    @Test
    void renovarFueraDeLaVentanaDevuelve409() throws Exception {
        Poliza poliza = polizas.save(Poliza.individual("Ana", "Carlos", "Calle 1", hoy, 12, new BigDecimal("1000")));

        mvc.perform(conClave(post("/polizas/{id}/renovar", poliza.getId()))).andExpect(status().isConflict());
    }

    @Test
    void sinIpcLaRenovacionDevuelve503YNoCambiaLaPoliza(CapturedOutput salida) throws Exception {
        Poliza poliza = individual();
        ipcRepository.deleteAll();

        mvc.perform(conClave(post("/polizas/{id}/renovar", poliza.getId())))
                .andExpect(status().isServiceUnavailable());

        Poliza guardada = polizas.findById(poliza.getId()).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo(EstadoPoliza.ACTIVA);
        assertThat(guardada.getCanon()).isEqualByComparingTo("1000");
        assertThat(salida).contains("Aun no hay IPC registrado");
        verify(core, never()).notificarActualizacion(any());
    }

    @Test
    void elIpcSeCargaManualmenteComoContingencia() throws Exception {
        Poliza poliza = individual();
        ipcRepository.deleteAll();
        int anio = hoy.getYear() - 1;

        mvc.perform(conClave(put("/ipc/{anio}", anio)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"porcentaje\": 5.1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuente").value("MANUAL"));
        mvc.perform(conClave(get("/ipc"))).andExpect(jsonPath("$[0].anio").value(anio));

        mvc.perform(conClave(post("/polizas/{id}/renovar", poliza.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canon").value(1051.00));
    }

    @Test
    void rechazaCargarIpcInvalido() throws Exception {
        mvc.perform(conClave(put("/ipc/{anio}", hoy.getYear() - 1)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"porcentaje\": 500}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("porcentaje"));
        mvc.perform(conClave(put("/ipc/{anio}", hoy.getYear())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"porcentaje\": 5}"))
                .andExpect(status().isConflict());
    }

    @Test
    void renovarUnaCanceladaDevuelve409YNoAvisaAlCore() throws Exception {
        Poliza poliza = individual();
        mvc.perform(conClave(post("/polizas/{id}/cancelar", poliza.getId()))).andExpect(status().isOk());
        clearInvocations(core);

        mvc.perform(conClave(post("/polizas/{id}/renovar", poliza.getId()))).andExpect(status().isConflict());

        verify(core, never()).notificarActualizacion(any());
    }

    @Test
    void cancelarLaPolizaPersisteLaCancelacionDeSusRiesgos() throws Exception {
        Poliza poliza = colectiva(2);

        mvc.perform(conClave(post("/polizas/{id}/cancelar", poliza.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        mvc.perform(conClave(get("/polizas/{id}/riesgos", poliza.getId())))
                .andExpect(jsonPath("$[0].estado").value("CANCELADO"))
                .andExpect(jsonPath("$[1].estado").value("CANCELADO"));
        verify(core).notificarActualizacion(poliza.getId());
    }

    @Test
    void agregaRiesgoAUnaColectiva() throws Exception {
        Poliza poliza = colectiva(1);

        mvc.perform(conClave(post("/polizas/{id}/riesgos", poliza.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(RIESGO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        mvc.perform(conClave(get("/polizas/{id}/riesgos", poliza.getId())))
                .andExpect(jsonPath("$.length()").value(2));
        verify(core).notificarActualizacion(poliza.getId());
    }

    @Test
    void agregarRiesgoAUnaIndividualDevuelve409() throws Exception {
        Poliza poliza = individual();

        mvc.perform(conClave(post("/polizas/{id}/riesgos", poliza.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(RIESGO_JSON))
                .andExpect(status().isConflict());

        mvc.perform(conClave(get("/polizas/{id}/riesgos", poliza.getId())))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void losErroresDeValidacionIndicanElCampo() throws Exception {
        Poliza poliza = colectiva(1);

        mvc.perform(conClave(post("/polizas/{id}/riesgos", poliza.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"arrendatario\": \" \", \"direccionInmueble\": \"Apto 1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("arrendatario"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("es obligatorio"));
    }

    @Test
    void unJsonInvalidoDevuelve400ConMensajeClaro() throws Exception {
        Poliza poliza = colectiva(1);

        mvc.perform(conClave(post("/polizas/{id}/riesgos", poliza.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content("{roto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El cuerpo debe ser un JSON valido"));
    }

    @Test
    void cancelaUnRiesgoDeUnaColectiva() throws Exception {
        Poliza poliza = colectiva(2);
        Long riesgoId = poliza.getRiesgos().getFirst().getId();

        mvc.perform(conClave(post("/riesgos/{id}/cancelar", riesgoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        mvc.perform(conClave(get("/polizas/{id}/riesgos", poliza.getId())))
                .andExpect(jsonPath("$[0].estado").value("CANCELADO"))
                .andExpect(jsonPath("$[1].estado").value("ACTIVO"));
        verify(core).notificarActualizacion(poliza.getId());
    }

    @Test
    void noPermiteCancelarDosVecesElMismoRiesgo() throws Exception {
        Long riesgoId = colectiva(2).getRiesgos().getFirst().getId();

        mvc.perform(conClave(post("/riesgos/{id}/cancelar", riesgoId))).andExpect(status().isOk());
        mvc.perform(conClave(post("/riesgos/{id}/cancelar", riesgoId))).andExpect(status().isConflict());
    }

    @Test
    void noPermiteCancelarElUltimoRiesgoActivo() throws Exception {
        Long riesgoId = colectiva(1).getRiesgos().getFirst().getId();

        mvc.perform(conClave(post("/riesgos/{id}/cancelar", riesgoId))).andExpect(status().isConflict());
    }

    @Test
    void noPermiteCancelarElRiesgoDeUnaIndividual() throws Exception {
        Long riesgoId = individual().getRiesgos().getFirst().getId();

        mvc.perform(conClave(post("/riesgos/{id}/cancelar", riesgoId))).andExpect(status().isConflict());
    }

    @Test
    void unFalloDelCoreNoRevierteLaOperacionConfirmada() throws Exception {
        Poliza poliza = individual();
        doThrow(new IllegalStateException("CORE caido")).when(core).notificarActualizacion(any());

        mvc.perform(conClave(post("/polizas/{id}/renovar", poliza.getId()))).andExpect(status().isOk());

        assertThat(polizas.findById(poliza.getId()).orElseThrow().getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
    }

    @Test
    void elMockDelCoreRegistraElEventoEnLogs(CapturedOutput salida) throws Exception {
        mvc.perform(conClave(post("/core-mock/evento")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evento\": \"ACTUALIZACION\", \"polizaId\": 555}"))
                .andExpect(status().isAccepted());

        assertThat(salida).contains("evento=ACTUALIZACION, polizaId=555");
    }

    @Test
    void elMockDelCoreRechazaEventosInvalidos() throws Exception {
        mvc.perform(conClave(post("/core-mock/evento")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evento\": \"X\\nFALSO\", \"polizaId\": 555}"))
                .andExpect(status().isBadRequest());
    }
}
