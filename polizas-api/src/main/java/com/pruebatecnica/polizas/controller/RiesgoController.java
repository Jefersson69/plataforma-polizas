package com.pruebatecnica.polizas.controller;

import com.pruebatecnica.polizas.dto.RiesgoResponse;
import com.pruebatecnica.polizas.service.PolizaService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/riesgos")
public class RiesgoController {

    private final PolizaService polizaService;

    public RiesgoController(PolizaService polizaService) {
        this.polizaService = polizaService;
    }

    @PostMapping("/{id}/cancelar")
    public RiesgoResponse cancelar(@PathVariable Long id) {
        return polizaService.cancelarRiesgo(id);
    }
}
