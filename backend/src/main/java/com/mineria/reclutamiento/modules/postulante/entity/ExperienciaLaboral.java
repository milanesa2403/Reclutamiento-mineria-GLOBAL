package com.mineria.reclutamiento.modules.postulante.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "experiencia_laboral")
public class ExperienciaLaboral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_experiencia")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_postulante", nullable = false)
    private Postulante postulante;

    @Column(name = "puesto", nullable = false, length = 150)
    private String puesto;

    @Column(name = "empresa", nullable = false, length = 150)
    private String empresa;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    protected ExperienciaLaboral() {
    }

    public ExperienciaLaboral(Postulante postulante, String puesto, String empresa, LocalDate fechaInicio,
                              LocalDate fechaFin, String descripcion) {
        this.postulante = postulante;
        this.puesto = puesto;
        this.empresa = empresa;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public Postulante getPostulante() { return postulante; }
    public String getPuesto() { return puesto; }
    public String getEmpresa() { return empresa; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public String getDescripcion() { return descripcion; }
}
