package com.mineria.reclutamiento.modules.postulante.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "habilidad_postulante")
public class HabilidadPostulante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_habilidad")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_postulante", nullable = false)
    private Postulante postulante;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    protected HabilidadPostulante() {
    }

    public HabilidadPostulante(Postulante postulante, String nombre) {
        this.postulante = postulante;
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public Postulante getPostulante() { return postulante; }
    public String getNombre() { return nombre; }
}
