package com.mineria.reclutamiento.modules.convocatoria.entity;

import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "busqueda")
public class Busqueda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_busqueda")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empresa", nullable = false, updatable = false)
    private Empresa empresa;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    protected Busqueda() {
    }

    public Busqueda(Empresa empresa, String nombre) {
        this.empresa = empresa;
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
