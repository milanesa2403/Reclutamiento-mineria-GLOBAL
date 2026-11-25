package com.mineria.reclutamiento.modelo;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Un trabajador solo puede postular una vez a cada oferta (restriccion unica = sin duplicados). */
@Entity
@Table(name = "postulaciones",
        uniqueConstraints = @UniqueConstraint(columnNames = {"oferta_id", "trabajador_id"}))
public class Postulacion {
    @Id
    private UUID id;
    @Column(name = "oferta_id", nullable = false)
    private UUID ofertaId;
    @Column(name = "trabajador_id", nullable = false)
    private UUID trabajadorId;
    private UUID documentoCvId;
    @Enumerated(EnumType.STRING)
    private EstadoPostulacion estado;
    private Instant fecha;

    protected Postulacion() {}

    public Postulacion(UUID id, UUID ofertaId, UUID trabajadorId, UUID documentoCvId) {
        this.id = id;
        this.ofertaId = ofertaId;
        this.trabajadorId = trabajadorId;
        this.documentoCvId = documentoCvId;
        this.estado = EstadoPostulacion.ENVIADA;
        this.fecha = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOfertaId() { return ofertaId; }
    public UUID getTrabajadorId() { return trabajadorId; }
    public UUID getDocumentoCvId() { return documentoCvId; }
    public EstadoPostulacion getEstado() { return estado; }
    public void setEstado(EstadoPostulacion estado) { this.estado = estado; }
    public Instant getFecha() { return fecha; }
}
