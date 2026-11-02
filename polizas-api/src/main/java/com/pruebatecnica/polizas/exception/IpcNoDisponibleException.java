package com.pruebatecnica.polizas.exception;

public class IpcNoDisponibleException extends RuntimeException {

    public IpcNoDisponibleException(int anio) {
        super("Aun no hay IPC registrado para " + anio + "; la renovacion queda pendiente");
    }
}
