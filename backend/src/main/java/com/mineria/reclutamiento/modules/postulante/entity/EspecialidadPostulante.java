package com.mineria.reclutamiento.modules.postulante.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "especialidad_postulante")
public class EspecialidadPostulante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especialidad")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_postulante", nullable = false)
    private Postulante postulante;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal;

    protected EspecialidadPostulante() {
    }

    public EspecialidadPostulante(Postulante postulante, String nombre, boolean esPrincipal) {
        this.postulante = postulante;
        this.nombre = nombre;
        this.esPrincipal = esPrincipal;
    }

    public Long getId() { return id; }
    public Postulante getPostulante() { return postulante; }
    public String getNombre() { return nombre; }
    public boolean isEsPrincipal() { return esPrincipal; }
}
