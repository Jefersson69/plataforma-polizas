package com.pruebatecnica.polizas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pruebatecnica.polizas.domain.IpcAnual;
import com.pruebatecnica.polizas.exception.IpcNoDisponibleException;
import com.pruebatecnica.polizas.ipc.IpcProvider;
import com.pruebatecnica.polizas.repository.IpcRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IpcServiceTest {

    private final IpcRepository repositorio = mock(IpcRepository.class);
    private final IpcProvider proveedor = mock(IpcProvider.class);
    private IpcService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC);
        servicio = new IpcService(repositorio, proveedor, reloj);
        when(proveedor.nombre()).thenReturn("DANE");
    }

    @Test
    void usaElIpcDelAnoCalendarioAnterior() {
        IpcAnual ipc = new IpcAnual(2025, new BigDecimal("5.10"), "DANE", LocalDate.of(2026, 1, 10));
        when(repositorio.findById(2025)).thenReturn(Optional.of(ipc));

        assertThat(servicio.aplicableHoy()).isSameAs(ipc);
    }

    @Test
    void siFaltaElAnoNoLoSustituyeConOtroValor() {
        when(repositorio.findById(2025)).thenReturn(Optional.empty());

        assertThatThrownBy(servicio::aplicableHoy).isInstanceOf(IpcNoDisponibleException.class);
    }

    @Test
    void unFalloDeLaFuenteNoRompeNiGuardaNada() {
        when(proveedor.obtenerVariacionesAnuales()).thenThrow(new IllegalStateException("DANE caido"));

        assertThat(servicio.sincronizar()).isZero();

        verify(repositorio, never()).save(any());
    }

    @Test
    void sincronizarSoloAgregaAnosCerradosQueFaltanYDescartaValoresNoPlausibles() {
        when(proveedor.obtenerVariacionesAnuales()).thenReturn(Map.of(
                2023, new BigDecimal("900"),
                2024, new BigDecimal("5.20"),
                2025, new BigDecimal("5.10"),
                2026, new BigDecimal("4.00")));
        when(repositorio.existsById(2024)).thenReturn(true);

        assertThat(servicio.sincronizar()).isEqualTo(1);

        verify(repositorio, times(1)).save(any());
        verify(repositorio).save(argThat(ipc -> ipc.getAnio() == 2025));
    }
}
