package com.pruebatecnica.polizas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poliza_id")
    private Poliza poliza;

    @Column(nullable = false)
    private String arrendatario;

    @Column(nullable = false)
    private String direccionInmueble;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRiesgo estado = EstadoRiesgo.ACTIVO;

    protected Riesgo() {
    }

    Riesgo(Poliza poliza, String arrendatario, String direccionInmueble) {
        Poliza.exigirTexto(arrendatario, "arrendatario");
        Poliza.exigirTexto(direccionInmueble, "direccionInmueble");
        this.poliza = poliza;
        this.arrendatario = arrendatario;
        this.direccionInmueble = direccionInmueble;
    }

    boolean estaActivo() {
        return estado == EstadoRiesgo.ACTIVO;
    }

    void cancelar() {
        estado = EstadoRiesgo.CANCELADO;
    }

    public Long getId() {
        return id;
    }

    public Poliza getPoliza() {
        return poliza;
    }

    public String getArrendatario() {
        return arrendatario;
    }

    public String getDireccionInmueble() {
        return direccionInmueble;
    }

    public EstadoRiesgo getEstado() {
        return estado;
    }
}
