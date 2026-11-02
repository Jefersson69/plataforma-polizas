package com.pruebatecnica.polizas.ipc;

import com.pruebatecnica.polizas.service.IpcService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ipc.sincronizacion.habilitada", havingValue = "true")
class IpcSincronizacionJob {

    private final IpcService ipcService;

    IpcSincronizacionJob(IpcService ipcService) {
        this.ipcService = ipcService;
    }

    @EventListener(ApplicationReadyEvent.class)
    void alArrancar() {
        ipcService.sincronizar();
    }

    @Scheduled(cron = "${ipc.sincronizacion.cron}", zone = "America/Bogota")
    void programado() {
        ipcService.sincronizar();
    }
}
