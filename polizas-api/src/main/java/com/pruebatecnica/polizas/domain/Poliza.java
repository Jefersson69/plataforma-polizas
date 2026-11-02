package com.pruebatecnica.polizas.domain;

import com.pruebatecnica.polizas.exception.ReglaNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Las reglas de negocio van aqui para que ningun servicio se las salte.
 */
@Entity
public class Poliza {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPoliza tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPoliza estado = EstadoPoliza.ACTIVA;

    @Column(nullable = false)
    private String tomador;

    @Column(nullable = false)
    private String beneficiario;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private int mesesVigencia;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal canon;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal prima;

    private Integer ipcAnioAplicado;

    @Column(precision = 5, scale = 2)
    private BigDecimal ipcPorcentajeAplicado;

    @OneToMany(mappedBy = "poliza", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Riesgo> riesgos = new ArrayList<>();

    protected Poliza() {
    }

    private Poliza(TipoPoliza tipo, String tomador, String beneficiario, LocalDate fechaInicio,
            int mesesVigencia, BigDecimal canon) {
        exigirTexto(tomador, "tomador");
        exigirTexto(beneficiario, "beneficiario");
        if (fechaInicio == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }
        if (mesesVigencia <= 0) {
            throw new IllegalArgumentException("La vigencia debe ser de al menos un mes");
        }
        if (canon == null || canon.signum() <= 0) {
            throw new IllegalArgumentException("El canon debe ser mayor que cero");
        }
        this.tipo = tipo;
        this.tomador = tomador;
        this.beneficiario = beneficiario;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaInicio.plusMonths(mesesVigencia);
        this.mesesVigencia = mesesVigencia;
        this.canon = canon.setScale(2, RoundingMode.HALF_UP);
        this.prima = calcularPrima();
    }

    /** En una individual el tomador es el arrendatario y nace con su unico riesgo. */
    public static Poliza individual(String arrendatario, String arrendador, String direccionInmueble,
            LocalDate fechaInicio, int mesesVigencia, BigDecimal canon) {
        Poliza poliza = new Poliza(TipoPoliza.INDIVIDUAL, arrendatario, arrendador, fechaInicio, mesesVigencia, canon);
        poliza.riesgos.add(new Riesgo(poliza, arrendatario, direccionInmueble));
        return poliza;
    }

    /** Una colectiva tiene uno o muchos riesgos, entonces nace con el primero. */
    public static Poliza colectiva(String tomador, String beneficiario, LocalDate fechaInicio, int mesesVigencia,
            BigDecimal canon, String arrendatario, String direccionInmueble) {
        Poliza poliza = new Poliza(TipoPoliza.COLECTIVA, tomador, beneficiario, fechaInicio, mesesVigencia, canon);
        poliza.riesgos.add(new Riesgo(poliza, arrendatario, direccionInmueble));
        return poliza;
    }

    /** Evita aplicar el IPC dos veces: al renovar, el vencimiento se aleja y se cierra la ventana. */
    public void verificarRenovable(LocalDate hoy, int ventanaDias) {
        exigirNoCancelada("renovar");
        LocalDate apertura = fechaFin.minusDays(ventanaDias);
        if (hoy.isBefore(apertura)) {
            throw new ReglaNegocioException("La poliza solo se puede renovar desde " + apertura);
        }
    }

    /** Renueva por el mismo periodo y recalcula la prima: canon x meses. */
    public void renovar(IpcAnual ipc, LocalDate hoy, int ventanaDias) {
        verificarRenovable(hoy, ventanaDias);
        canon = canon.multiply(CIEN.add(ipc.getPorcentaje())).divide(CIEN, 2, RoundingMode.HALF_UP);
        prima = calcularPrima();
        fechaInicio = fechaFin;
        fechaFin = fechaInicio.plusMonths(mesesVigencia);
        ipcAnioAplicado = ipc.getAnio();
        ipcPorcentajeAplicado = ipc.getPorcentaje();
        estado = EstadoPoliza.RENOVADA;
    }

    public void cancelar() {
        exigirNoCancelada("cancelar");
        estado = EstadoPoliza.CANCELADA;
        riesgos.forEach(Riesgo::cancelar);
    }

    public Riesgo agregarRiesgo(String arrendatario, String direccionInmueble) {
        if (tipo != TipoPoliza.COLECTIVA) {
            throw new ReglaNegocioException("Solo se pueden agregar riesgos a polizas colectivas");
        }
        exigirNoCancelada("agregar riesgos a");
        Riesgo riesgo = new Riesgo(this, arrendatario, direccionInmueble);
        riesgos.add(riesgo);
        return riesgo;
    }

    /** Siempre debe quedar un riesgo activo; si no, toca cancelar la poliza. */
    public Riesgo cancelarRiesgo(Long riesgoId) {
        if (tipo != TipoPoliza.COLECTIVA) {
            throw new ReglaNegocioException("El riesgo de una poliza individual solo se cancela cancelando la poliza");
        }
        Riesgo riesgo = riesgos.stream()
                .filter(r -> r.getId().equals(riesgoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El riesgo no pertenece a la poliza"));
        if (!riesgo.estaActivo()) {
            throw new ReglaNegocioException("El riesgo ya esta cancelado");
        }
        if (riesgos.stream().filter(Riesgo::estaActivo).count() == 1) {
            throw new ReglaNegocioException("No se puede cancelar el ultimo riesgo activo; cancele la poliza");
        }
        riesgo.cancelar();
        return riesgo;
    }

    private void exigirNoCancelada(String accion) {
        if (estado == EstadoPoliza.CANCELADA) {
            throw new ReglaNegocioException("No se puede " + accion + " una poliza cancelada");
        }
    }

    static void exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
    }

    private BigDecimal calcularPrima() {
        return canon.multiply(BigDecimal.valueOf(mesesVigencia));
    }

    public Long getId() {
        return id;
    }

    public TipoPoliza getTipo() {
        return tipo;
    }

    public EstadoPoliza getEstado() {
        return estado;
    }

    public String getTomador() {
        return tomador;
    }

    public String getBeneficiario() {
        return beneficiario;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public int getMesesVigencia() {
        return mesesVigencia;
    }

    public BigDecimal getCanon() {
        return canon;
    }

    public BigDecimal getPrima() {
        return prima;
    }

    public Integer getIpcAnioAplicado() {
        return ipcAnioAplicado;
    }

    public BigDecimal getIpcPorcentajeAplicado() {
        return ipcPorcentajeAplicado;
    }

    public List<Riesgo> getRiesgos() {
        return List.copyOf(riesgos);
    }
}
