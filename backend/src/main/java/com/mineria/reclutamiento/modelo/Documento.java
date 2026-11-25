package com.mineria.reclutamiento.modelo;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Un solo original fisico por hash SHA-256 (evita duplicados); cada fila enlaza a un propietario. */
@Entity
@Table(name = "documentos")
public class Documento {
    @Id
    private UUID id;
    private UUID propietarioId;
    @Enumerated(EnumType.STRING)
    private TipoDocumento tipo;
    private String nombreArchivo;
    @Column(length = 64)
    private String sha256;
    private long tamanioBytes;
    @Enumerated(EnumType.STRING)
    private EstadoDocumento estado = EstadoDocumento.PROCESADO;
    private Instant fechaSubida;

    protected Documento() {}

    public Documento(UUID id, UUID propietarioId, TipoDocumento tipo, String nombreArchivo, String sha256, long tamanioBytes) {
        this.id = id;
        this.propietarioId = propietarioId;
        this.tipo = tipo;
        this.nombreArchivo = nombreArchivo;
        this.sha256 = sha256;
        this.tamanioBytes = tamanioBytes;
        this.fechaSubida = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPropietarioId() { return propietarioId; }
    public TipoDocumento getTipo() { return tipo; }
    public String getNombreArchivo() { return nombreArchivo; }
    public String getSha256() { return sha256; }
    public long getTamanioBytes() { return tamanioBytes; }
    public EstadoDocumento getEstado() { return estado; }
    public void setEstado(EstadoDocumento estado) { this.estado = estado; }
    public Instant getFechaSubida() { return fechaSubida; }
}
