package com.pruebatecnica.polizas.service;

import com.pruebatecnica.polizas.domain.IpcAnual;
import com.pruebatecnica.polizas.dto.IpcResponse;
import com.pruebatecnica.polizas.exception.IpcNoDisponibleException;
import com.pruebatecnica.polizas.ipc.IpcProvider;
import com.pruebatecnica.polizas.repository.IpcRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IpcService {

    private static final Logger log = LoggerFactory.getLogger(IpcService.class);
    private static final String FUENTE_MANUAL = "MANUAL";

    private final IpcRepository ipcRepository;
    private final IpcProvider proveedor;
    private final Clock reloj;

    public IpcService(IpcRepository ipcRepository, IpcProvider proveedor, Clock reloj) {
        this.ipcRepository = ipcRepository;
        this.proveedor = proveedor;
        this.reloj = reloj;
    }

    /** Usa el IPC del ano anterior, nunca otro ano ni un valor fijo. */
    @Transactional(readOnly = true)
    public IpcAnual aplicableHoy() {
        int anio = LocalDate.now(reloj).getYear() - 1;
        return ipcRepository.findById(anio).orElseThrow(() -> new IpcNoDisponibleException(anio));
    }

    @Transactional(readOnly = true)
    public List<IpcResponse> listar() {
        return ipcRepository.findAllByOrderByAnioDesc().stream().map(IpcResponse::desde).toList();
    }

    /** Carga manual, para cuando falla la fuente o hay que corregir un valor. */
    @Transactional
    public IpcResponse registrarManual(int anio, BigDecimal porcentaje) {
        LocalDate hoy = LocalDate.now(reloj);
        IpcAnual ipc = ipcRepository.findById(anio)
                .map(existente -> {
                    log.warn("IPC {} cambia manualmente de {} a {}", anio, existente.getPorcentaje(), porcentaje);
                    existente.actualizar(porcentaje, FUENTE_MANUAL, hoy);
                    return existente;
                })
                .orElseGet(() -> ipcRepository.save(new IpcAnual(anio, porcentaje, FUENTE_MANUAL, hoy)));
        return IpcResponse.desde(ipc);
    }

    /** Solo agrega los anos que faltan, no reemplaza lo que ya esta. */
    public int sincronizar() {
        Map<Integer, BigDecimal> variaciones;
        try {
            variaciones = proveedor.obtenerVariacionesAnuales();
        } catch (RuntimeException ex) {
            log.error("No se pudo consultar el IPC en {}; se reintentara en la siguiente ejecucion", proveedor.nombre(), ex);
            return 0;
        }
        LocalDate hoy = LocalDate.now(reloj);
        int agregados = 0;
        for (Map.Entry<Integer, BigDecimal> dato : variaciones.entrySet()) {
            int anio = dato.getKey();
            if (anio >= hoy.getYear() || ipcRepository.existsById(anio)) {
                continue;
            }
            if (!IpcAnual.esPlausible(dato.getValue())) {
                log.error("IPC {} descartado por valor no plausible: {}", anio, dato.getValue());
                continue;
            }
            ipcRepository.save(new IpcAnual(anio, dato.getValue(), proveedor.nombre(), hoy));
            agregados++;
        }
        log.info("Sincronizacion de IPC desde {}: {} anos agregados", proveedor.nombre(), agregados);
        return agregados;
    }
}
