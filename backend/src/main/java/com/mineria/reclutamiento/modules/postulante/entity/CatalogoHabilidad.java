package com.mineria.reclutamiento.modules.postulante.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalogo_habilidad")
public class CatalogoHabilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "categoria", length = 50)
    private String categoria;

    protected CatalogoHabilidad() {
    }

    public Integer getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
}
