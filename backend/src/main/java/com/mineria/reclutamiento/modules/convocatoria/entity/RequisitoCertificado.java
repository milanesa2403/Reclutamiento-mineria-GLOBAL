package com.mineria.reclutamiento.modules.convocatoria.entity;

import com.mineria.reclutamiento.modules.postulante.entity.CatalogoCertificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "convocatoria_requisito_certificado")
public class RequisitoCertificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_requisito")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_convocatoria", nullable = false, updatable = false)
    private Convocatoria convocatoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_catalogo_certificacion", nullable = false, updatable = false)
    private CatalogoCertificacion catalogo;

    @Column(name = "requiere_pdf", nullable = false)
    private boolean requierePdf;

    protected RequisitoCertificado() {
    }

    public RequisitoCertificado(Convocatoria convocatoria, CatalogoCertificacion catalogo, boolean requierePdf) {
        this.convocatoria = convocatoria;
        this.catalogo = catalogo;
        this.requierePdf = requierePdf;
    }

    public Long getId() { return id; }
    public Convocatoria getConvocatoria() { return convocatoria; }
    public CatalogoCertificacion getCatalogo() { return catalogo; }
    public String getNombre() { return catalogo.getNombre(); }
    public boolean isRequierePdf() { return requierePdf; }
}
