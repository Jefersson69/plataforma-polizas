package com.pruebatecnica.polizas.dto;

import com.pruebatecnica.polizas.domain.EstadoRiesgo;
import com.pruebatecnica.polizas.domain.Riesgo;

public record RiesgoResponse(Long id, Long polizaId, String arrendatario, String direccionInmueble, EstadoRiesgo estado) {

    public static RiesgoResponse desde(Riesgo r) {
        return new RiesgoResponse(r.getId(), r.getPoliza().getId(), r.getArrendatario(), r.getDireccionInmueble(),
                r.getEstado());
    }
}
