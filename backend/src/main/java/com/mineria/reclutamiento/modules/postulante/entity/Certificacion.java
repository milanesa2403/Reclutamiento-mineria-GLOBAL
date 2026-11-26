package com.mineria.reclutamiento.modules.postulante.entity;

import jakarta.persistence.*;
import com.mineria.reclutamiento.modules.documento.entity.Documento;
import java.time.LocalDate;

@Entity
@Table(name = "certificacion")
public class Certificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_certificacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_postulante", nullable = false)
    private Postulante postulante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_catalogo_certificacion")
    private CatalogoCertificacion catalogo;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "entidad_emisora", nullable = false, length = 150)
    private String entidadEmisora;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_documento", unique = true)
    private Documento documento;

    protected Certificacion() {
    }

    public Certificacion(Postulante postulante, CatalogoCertificacion catalogo, String nombre, String entidadEmisora,
                         LocalDate fechaEmision) {
        this.postulante = postulante;
        this.catalogo = catalogo;
        this.nombre = nombre;
        this.entidadEmisora = entidadEmisora;
        this.fechaEmision = fechaEmision;
    }

    public void actualizar(CatalogoCertificacion catalogo, String nombre, String entidadEmisora, LocalDate fechaEmision) {
        this.catalogo = catalogo;
        this.nombre = nombre;
        this.entidadEmisora = entidadEmisora;
        this.fechaEmision = fechaEmision;
    }

    public Long getId() { return id; }
    public Postulante getPostulante() { return postulante; }
    public CatalogoCertificacion getCatalogo() { return catalogo; }
    public String getNombre() { return nombre; }
    public String getEntidadEmisora() { return entidadEmisora; }
    public LocalDate getFechaEmision() { return fechaEmision; }
    public Documento getDocumento() { return documento; }
    public void setDocumento(Documento documento) { this.documento = documento; }
}
