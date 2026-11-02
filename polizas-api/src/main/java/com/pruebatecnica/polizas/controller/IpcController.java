package com.pruebatecnica.polizas.controller;

import com.pruebatecnica.polizas.dto.IpcRequest;
import com.pruebatecnica.polizas.dto.IpcResponse;
import com.pruebatecnica.polizas.service.IpcService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ipc")
public class IpcController {

    private final IpcService ipcService;

    public IpcController(IpcService ipcService) {
        this.ipcService = ipcService;
    }

    @GetMapping
    public List<IpcResponse> listar() {
        return ipcService.listar();
    }

    @PutMapping("/{anio}")
    public IpcResponse registrar(@PathVariable int anio, @Valid @RequestBody IpcRequest request) {
        return ipcService.registrarManual(anio, request.porcentaje());
    }
}
