package com.mineria.reclutamiento.modules.postulacion.entity;

import com.mineria.reclutamiento.modules.convocatoria.entity.RequisitoCertificado;
import com.mineria.reclutamiento.modules.postulante.entity.Certificacion;
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
@Table(name = "postulacion_requisito")
public class PostulacionRequisito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_postulacion_requisito")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_postulacion", nullable = false, updatable = false)
    private Postulacion postulacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_requisito", nullable = false, updatable = false)
    private RequisitoCertificado requisito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_certificacion", nullable = false, updatable = false)
    private Certificacion certificacion;

    protected PostulacionRequisito() {
    }

    public PostulacionRequisito(Postulacion postulacion, RequisitoCertificado requisito, Certificacion certificacion) {
        this.postulacion = postulacion;
        this.requisito = requisito;
        this.certificacion = certificacion;
    }

    public Long getId() { return id; }
    public Postulacion getPostulacion() { return postulacion; }
    public RequisitoCertificado getRequisito() { return requisito; }
    public Certificacion getCertificacion() { return certificacion; }
}
