package com.pruebatecnica.polizas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pruebatecnica.polizas.exception.ReglaNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PolizaTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
    private static final LocalDate VENCIMIENTO = LocalDate.of(2027, 1, 1);
    private static final LocalDate HOY = LocalDate.of(2026, 12, 15);
    private static final int VENTANA = 60;

    private static IpcAnual ipc(String porcentaje) {
        return new IpcAnual(2025, new BigDecimal(porcentaje), "PRUEBA", HOY);
    }

    private static Poliza individual() {
        return Poliza.individual("Ana", "Carlos", "Calle 1", INICIO, 12, new BigDecimal("1000000"));
    }

    private static Poliza colectiva() {
        return Poliza.colectiva("Inmobiliaria", "Propietarios", INICIO, 6, new BigDecimal("1000000"), "Luis", "Apto 1");
    }

    @Test
    void laPrimaEsElCanonPorLosMesesDeVigencia() {
        assertThat(individual().getPrima()).isEqualByComparingTo("12000000");
    }

    @Test
    void laIndividualNaceConExactamenteUnRiesgo() {
        assertThat(individual().getRiesgos()).hasSize(1);
    }

    @Test
    void laColectivaNaceConSuPrimerRiesgo() {
        assertThat(colectiva().getRiesgos()).hasSize(1);
    }

    @Test
    void rechazaCanonInvalido() {
        assertThatThrownBy(() -> Poliza.individual("Ana", "Carlos", "Calle 1", INICIO, 12, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Poliza.individual("Ana", "Carlos", "Calle 1", INICIO, 12, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaDatosObligatoriosVacios() {
        BigDecimal canon = BigDecimal.TEN;
        assertThatThrownBy(() -> Poliza.individual(" ", "Carlos", "Calle 1", INICIO, 12, canon))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Poliza.individual("Ana", null, "Calle 1", INICIO, 12, canon))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Poliza.colectiva("I", "P", INICIO, 6, canon, "Luis", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaIpcNoPlausible() {
        assertThatThrownBy(() -> ipc("-200")).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> ipc("101")).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void soloAceptaIpcDeAnosCerrados() {
        assertThatThrownBy(() -> new IpcAnual(2026, BigDecimal.TEN, "PRUEBA", HOY))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void renovarAplicaIpcYExtiendeLaVigenciaPorElMismoPeriodo() {
        Poliza poliza = individual();

        poliza.renovar(ipc("5.2"), HOY, VENTANA);

        assertThat(poliza.getCanon()).isEqualByComparingTo("1052000");
        assertThat(poliza.getPrima()).isEqualByComparingTo("12624000");
        assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
        assertThat(poliza.getFechaInicio()).isEqualTo(VENCIMIENTO);
        assertThat(poliza.getFechaFin()).isEqualTo(LocalDate.of(2028, 1, 1));
        assertThat(poliza.getIpcAnioAplicado()).isEqualTo(2025);
        assertThat(poliza.getIpcPorcentajeAplicado()).isEqualByComparingTo("5.2");
    }

    @Test
    void laPrimaSigueCuadrandoConElCanonRedondeado() {
        Poliza poliza = Poliza.individual("Ana", "Carlos", "Calle 1", INICIO, 12, new BigDecimal("333.33"));

        poliza.renovar(ipc("5.25"), HOY, VENTANA);

        assertThat(poliza.getCanon()).isEqualByComparingTo("350.83");
        assertThat(poliza.getPrima()).isEqualByComparingTo("4209.96");
    }

    @Test
    void noSePuedeRenovarAntesDeLaVentana() {
        Poliza poliza = individual();
        LocalDate apertura = VENCIMIENTO.minusDays(VENTANA);

        assertThatThrownBy(() -> poliza.renovar(ipc("10"), apertura.minusDays(1), VENTANA))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(poliza.getCanon()).isEqualByComparingTo("1000000");

        poliza.renovar(ipc("10"), apertura, VENTANA);
        assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.RENOVADA);
    }

    @Test
    void unaRenovacionRepetidaNoAplicaElIpcDosVeces() {
        Poliza poliza = individual();
        poliza.renovar(ipc("10"), HOY, VENTANA);

        assertThatThrownBy(() -> poliza.renovar(ipc("10"), HOY, VENTANA))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(poliza.getCanon()).isEqualByComparingTo("1100000");
    }

    @Test
    void unaPolizaRenovadaSePuedeRenovarEnSuSiguienteVencimiento() {
        Poliza poliza = individual();
        poliza.renovar(ipc("10"), HOY, VENTANA);

        poliza.renovar(ipc("10"), LocalDate.of(2027, 12, 15), VENTANA);

        assertThat(poliza.getCanon()).isEqualByComparingTo("1210000");
    }

    @Test
    void noSePuedeRenovarUnaPolizaCancelada() {
        Poliza poliza = individual();
        poliza.cancelar();

        assertThatThrownBy(() -> poliza.renovar(ipc("10"), HOY, VENTANA)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void cancelarLaPolizaCancelaTodosSusRiesgos() {
        Poliza poliza = colectiva();
        poliza.agregarRiesgo("Marta", "Apto 2");

        poliza.cancelar();

        assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
        assertThat(poliza.getRiesgos()).allMatch(r -> r.getEstado() == EstadoRiesgo.CANCELADO);
    }

    @Test
    void noSePuedeCancelarDosVeces() {
        Poliza poliza = individual();
        poliza.cancelar();

        assertThatThrownBy(poliza::cancelar).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void noSePuedenAgregarRiesgosAUnaIndividual() {
        Poliza poliza = individual();

        assertThatThrownBy(() -> poliza.agregarRiesgo("Luis", "Apto 1")).isInstanceOf(ReglaNegocioException.class);
        assertThat(poliza.getRiesgos()).hasSize(1);
    }

    @Test
    void noSePuedenAgregarRiesgosAUnaColectivaCancelada() {
        Poliza poliza = colectiva();
        poliza.cancelar();

        assertThatThrownBy(() -> poliza.agregarRiesgo("Marta", "Apto 2")).isInstanceOf(ReglaNegocioException.class);
    }
}
