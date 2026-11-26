package com.mineria.reclutamiento.modules.convocatoria.entity;

import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
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
@Table(name = "convocatoria_guardada")
public class ConvocatoriaGuardada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_convocatoria_guardada")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_postulante", nullable = false, updatable = false)
    private Postulante postulante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_convocatoria", nullable = false, updatable = false)
    private Convocatoria convocatoria;

    @CreationTimestamp
    @Column(name = "fecha_guardado", nullable = false, updatable = false)
    private LocalDateTime fechaGuardado;

    protected ConvocatoriaGuardada() {
    }

    public ConvocatoriaGuardada(Postulante postulante, Convocatoria convocatoria) {
        this.postulante = postulante;
        this.convocatoria = convocatoria;
    }

    public Long getId() { return id; }
    public Postulante getPostulante() { return postulante; }
    public Convocatoria getConvocatoria() { return convocatoria; }
    public LocalDateTime getFechaGuardado() { return fechaGuardado; }
}
