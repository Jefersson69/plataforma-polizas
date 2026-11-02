package com.pruebatecnica.polizas.dto;

import com.pruebatecnica.polizas.domain.EstadoPoliza;
import com.pruebatecnica.polizas.domain.Poliza;
import com.pruebatecnica.polizas.domain.TipoPoliza;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PolizaResponse(
        Long id,
        TipoPoliza tipo,
        EstadoPoliza estado,
        String tomador,
        String beneficiario,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int mesesVigencia,
        BigDecimal canon,
        BigDecimal prima,
        Integer ipcAnioAplicado,
        BigDecimal ipcPorcentajeAplicado) {

    public static PolizaResponse desde(Poliza p) {
        return new PolizaResponse(p.getId(), p.getTipo(), p.getEstado(), p.getTomador(), p.getBeneficiario(),
                p.getFechaInicio(), p.getFechaFin(), p.getMesesVigencia(), p.getCanon(), p.getPrima(),
                p.getIpcAnioAplicado(), p.getIpcPorcentajeAplicado());
    }
}
