package com.mineria.reclutamiento.modules.postulante.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "formacion_academica")
public class FormacionAcademica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_formacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_postulante", nullable = false)
    private Postulante postulante;

    @Column(name = "institucion", nullable = false, length = 200)
    private String institucion;

    @Column(name = "carrera_grado", nullable = false, length = 150)
    private String carreraGrado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    protected FormacionAcademica() {
    }

    public FormacionAcademica(Postulante postulante, String institucion, String carreraGrado, LocalDate fechaInicio,
                              LocalDate fechaFin) {
        this.postulante = postulante;
        this.institucion = institucion;
        this.carreraGrado = carreraGrado;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public Long getId() { return id; }
    public Postulante getPostulante() { return postulante; }
    public String getInstitucion() { return institucion; }
    public String getCarreraGrado() { return carreraGrado; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
}
