package com.pruebatecnica.polizas.domain;

import com.pruebatecnica.polizas.exception.ReglaNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
public class IpcAnual {

    private static final int PRIMER_ANIO = 2000;

    @Id
    private Integer anio;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(nullable = false)
    private String fuente;

    @Column(nullable = false)
    private LocalDate fechaRegistro;

    protected IpcAnual() {
    }

    public IpcAnual(int anio, BigDecimal porcentaje, String fuente, LocalDate hoy) {
        this.anio = anio;
        actualizar(porcentaje, fuente, hoy);
    }

    public void actualizar(BigDecimal nuevoPorcentaje, String nuevaFuente, LocalDate hoy) {
        if (anio < PRIMER_ANIO || anio >= hoy.getYear()) {
            throw new ReglaNegocioException("El ano del IPC debe estar cerrado y ser desde " + PRIMER_ANIO);
        }
        if (!esPlausible(nuevoPorcentaje)) {
            throw new ReglaNegocioException("El IPC debe estar entre 0 y 100");
        }
        porcentaje = nuevoPorcentaje.setScale(2, RoundingMode.HALF_UP);
        fuente = nuevaFuente;
        fechaRegistro = hoy;
    }

    public static boolean esPlausible(BigDecimal valor) {
        return valor != null && valor.signum() >= 0 && valor.compareTo(BigDecimal.valueOf(100)) <= 0;
    }

    public Integer getAnio() {
        return anio;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public String getFuente() {
        return fuente;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }
}
