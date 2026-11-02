package com.pruebatecnica.polizas.controller;

import com.pruebatecnica.polizas.dto.CoreEventoRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CoreMockController {

    private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);

    @PostMapping("/core-mock/evento")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void recibirEvento(@Valid @RequestBody CoreEventoRequest evento) {
        log.info("Intento de envio al CORE: evento={}, polizaId={}", evento.evento(), evento.polizaId());
    }
}
