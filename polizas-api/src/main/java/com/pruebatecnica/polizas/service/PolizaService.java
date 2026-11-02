package com.pruebatecnica.polizas.service;

import com.pruebatecnica.polizas.config.RenovacionProperties;
import com.pruebatecnica.polizas.core.PolizaModificada;
import com.pruebatecnica.polizas.domain.EstadoPoliza;
import com.pruebatecnica.polizas.domain.Poliza;
import com.pruebatecnica.polizas.domain.Riesgo;
import com.pruebatecnica.polizas.domain.TipoPoliza;
import com.pruebatecnica.polizas.dto.CrearRiesgoRequest;
import com.pruebatecnica.polizas.dto.PolizaResponse;
import com.pruebatecnica.polizas.dto.RiesgoResponse;
import com.pruebatecnica.polizas.exception.RecursoNoEncontradoException;
import com.pruebatecnica.polizas.repository.PolizaRepository;
import com.pruebatecnica.polizas.repository.RiesgoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolizaService {

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final IpcService ipcService;
    private final ApplicationEventPublisher eventos;
    private final RenovacionProperties renovacion;
    private final Clock reloj;

    public PolizaService(PolizaRepository polizaRepository, RiesgoRepository riesgoRepository, IpcService ipcService,
            ApplicationEventPublisher eventos, RenovacionProperties renovacion, Clock reloj) {
        this.polizaRepository = polizaRepository;
        this.riesgoRepository = riesgoRepository;
        this.ipcService = ipcService;
        this.eventos = eventos;
        this.renovacion = renovacion;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<PolizaResponse> listar(TipoPoliza tipo, EstadoPoliza estado) {
        return polizaRepository.buscar(tipo, estado).stream().map(PolizaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<RiesgoResponse> listarRiesgos(Long polizaId) {
        if (!polizaRepository.existsById(polizaId)) {
            throw polizaNoEncontrada(polizaId);
        }
        return riesgoRepository.findByPolizaIdOrderById(polizaId).stream().map(RiesgoResponse::desde).toList();
    }

    @Transactional
    public PolizaResponse renovar(Long polizaId) {
        Poliza poliza = obtenerParaActualizar(polizaId);
        LocalDate hoy = LocalDate.now(reloj);
        poliza.verificarRenovable(hoy, renovacion.ventanaDias());
        poliza.renovar(ipcService.aplicableHoy(), hoy, renovacion.ventanaDias());
        eventos.publishEvent(new PolizaModificada(polizaId));
        return PolizaResponse.desde(poliza);
    }

    @Transactional
    public PolizaResponse cancelar(Long polizaId) {
        Poliza poliza = obtenerParaActualizar(polizaId);
        poliza.cancelar();
        eventos.publishEvent(new PolizaModificada(polizaId));
        return PolizaResponse.desde(poliza);
    }

    @Transactional
    public RiesgoResponse agregarRiesgo(Long polizaId, CrearRiesgoRequest request) {
        Poliza poliza = obtenerParaActualizar(polizaId);
        Riesgo riesgo = riesgoRepository.save(poliza.agregarRiesgo(request.arrendatario(), request.direccionInmueble()));
        eventos.publishEvent(new PolizaModificada(polizaId));
        return RiesgoResponse.desde(riesgo);
    }

    @Transactional
    public RiesgoResponse cancelarRiesgo(Long riesgoId) {
        Long polizaId = riesgoRepository.buscarPolizaId(riesgoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el riesgo " + riesgoId));
        Riesgo riesgo = obtenerParaActualizar(polizaId).cancelarRiesgo(riesgoId);
        eventos.publishEvent(new PolizaModificada(polizaId));
        return RiesgoResponse.desde(riesgo);
    }

    private Poliza obtenerParaActualizar(Long polizaId) {
        return polizaRepository.buscarParaActualizar(polizaId).orElseThrow(() -> polizaNoEncontrada(polizaId));
    }

    private static RecursoNoEncontradoException polizaNoEncontrada(Long polizaId) {
        return new RecursoNoEncontradoException("No existe la poliza " + polizaId);
    }
}
