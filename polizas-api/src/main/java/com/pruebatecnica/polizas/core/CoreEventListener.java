package com.pruebatecnica.polizas.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Avisa al CORE despues del commit, para no informar cambios revertidos. Si falla, queda en el log.
 * Falta: la conexion sigue ocupada durante la llamada y no hay reintentos. Se resuelve con el outbox del Modulo 1.
 */
@Component
class CoreEventListener {

    private static final Logger log = LoggerFactory.getLogger(CoreEventListener.class);

    private final CoreClient coreClient;

    CoreEventListener(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void alModificarPoliza(PolizaModificada evento) {
        try {
            coreClient.notificarActualizacion(evento.polizaId());
        } catch (RuntimeException ex) {
            log.error("No se pudo notificar al CORE la poliza {}", evento.polizaId(), ex);
        }
    }
}
