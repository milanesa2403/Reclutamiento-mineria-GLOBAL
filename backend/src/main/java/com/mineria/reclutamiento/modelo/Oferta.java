package com.mineria.reclutamiento.modelo;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Fechas en UTC (ISO 8601), pais ISO 3166-1, moneda ISO 4217. */
@Entity
@Table(name = "ofertas")
public class Oferta {
    @Id
    private UUID id;
    private UUID empresaId;
    private String titulo;
    @Column(length = 4000)
    private String descripcion;
    private String especialidadRequerida;
    private String pais;
    private double salario;
    private String moneda;
    @Enumerated(EnumType.STRING)
    private EstadoOferta estado;
    private Instant fechaPublicacion;
    private Instant fechaCierre;

    protected Oferta() {}

    public Oferta(UUID id, UUID empresaId, String titulo) {
        this.id = id;
        this.empresaId = empresaId;
        this.titulo = titulo;
        this.estado = EstadoOferta.ABIERTA;
        this.fechaPublicacion = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEmpresaId() { return empresaId; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String d) { this.descripcion = d; }
    public String getEspecialidadRequerida() { return especialidadRequerida; }
    public void setEspecialidadRequerida(String e) { this.especialidadRequerida = e; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public double getSalario() { return salario; }
    public void setSalario(double salario) { this.salario = salario; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public EstadoOferta getEstado() { return estado; }
    public void setEstado(EstadoOferta estado) { this.estado = estado; }
    public Instant getFechaPublicacion() { return fechaPublicacion; }
    public Instant getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(Instant f) { this.fechaCierre = f; }
}
