package com.pruebatecnica.polizas.config;

import com.pruebatecnica.polizas.domain.IpcAnual;
import com.pruebatecnica.polizas.domain.Poliza;
import com.pruebatecnica.polizas.domain.TipoPoliza;
import com.pruebatecnica.polizas.repository.IpcRepository;
import com.pruebatecnica.polizas.repository.PolizaRepository;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import tools.jackson.databind.ObjectMapper;

/**
 * Datos de demo, solo en local, porque la prueba no tiene endpoint de creacion.
 * Se crean con el dominio, asi cumplen las reglas, y las fechas salen de hoy para que no se venzan.
 */
@Configuration
@ConditionalOnProperty(name = "polizas.datos-iniciales", havingValue = "true")
class DatosIniciales {

    record DatosDemo(BigDecimal ipcPorcentaje, List<PolizaDemo> polizas) {
    }

    record PolizaDemo(TipoPoliza tipo, String tomador, String beneficiario, int mesesVigencia, BigDecimal canon,
            int venceEnDias, Boolean cancelada, List<RiesgoDemo> riesgos) {
    }

    record RiesgoDemo(String arrendatario, String direccionInmueble) {
    }

    @Bean
    ApplicationRunner cargarDatosDeEjemplo(PolizaRepository polizas, IpcRepository ipc, Clock reloj,
            ObjectMapper json, @Value("classpath:datos-demo.json") Resource archivo) {
        return args -> {
            if (polizas.count() > 0) {
                return;
            }
            DatosDemo datos;
            try (InputStream contenido = archivo.getInputStream()) {
                datos = json.readValue(contenido, DatosDemo.class);
            }
            LocalDate hoy = LocalDate.now(reloj);
            int anioIpc = hoy.getYear() - 1;
            if (!ipc.existsById(anioIpc)) {
                ipc.save(new IpcAnual(anioIpc, datos.ipcPorcentaje(), "EJEMPLO", hoy));
            }
            polizas.saveAll(datos.polizas().stream().map(demo -> aPoliza(demo, hoy)).toList());
        };
    }

    private static Poliza aPoliza(PolizaDemo demo, LocalDate hoy) {
        LocalDate inicio = hoy.plusDays(demo.venceEnDias()).minusMonths(demo.mesesVigencia());
        RiesgoDemo primero = demo.riesgos().getFirst();
        Poliza poliza = demo.tipo() == TipoPoliza.INDIVIDUAL
                ? Poliza.individual(demo.tomador(), demo.beneficiario(), primero.direccionInmueble(),
                        inicio, demo.mesesVigencia(), demo.canon())
                : Poliza.colectiva(demo.tomador(), demo.beneficiario(), inicio, demo.mesesVigencia(),
                        demo.canon(), primero.arrendatario(), primero.direccionInmueble());
        demo.riesgos().stream().skip(1)
                .forEach(riesgo -> poliza.agregarRiesgo(riesgo.arrendatario(), riesgo.direccionInmueble()));
        if (Boolean.TRUE.equals(demo.cancelada())) {
            poliza.cancelar();
        }
        return poliza;
    }
}
