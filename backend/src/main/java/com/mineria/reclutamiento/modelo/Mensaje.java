package com.mineria.reclutamiento.modelo;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mensajes")
public class Mensaje {
    @Id
    private UUID id;
    private UUID postulacionId;
    private UUID remitenteId;
    @Column(length = 2000)
    private String contenido;
    private Instant fecha;

    protected Mensaje() {}

    public Mensaje(UUID id, UUID postulacionId, UUID remitenteId, String contenido) {
        this.id = id;
        this.postulacionId = postulacionId;
        this.remitenteId = remitenteId;
        this.contenido = contenido;
        this.fecha = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPostulacionId() { return postulacionId; }
    public UUID getRemitenteId() { return remitenteId; }
    public String getContenido() { return contenido; }
    public Instant getFecha() { return fecha; }
}
