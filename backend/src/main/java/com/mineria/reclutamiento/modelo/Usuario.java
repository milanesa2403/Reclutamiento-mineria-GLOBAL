package com.mineria.reclutamiento.modelo;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    private UUID id;
    @Column(unique = true, nullable = false)
    private String correo;
    @Column(nullable = false)
    private String claveHash;
    @Enumerated(EnumType.STRING)
    private Rol rol;
    private boolean segundoFactorActivo;
    private int intentosFallidos;
    private Instant bloqueadoHasta;
    private Instant consentimientoEn;
    private Instant borradoSolicitadoEn;

    protected Usuario() {}

    public Usuario(UUID id, String correo, Rol rol) {
        this.id = id;
        this.correo = correo;
        this.rol = rol;
    }

    public UUID getId() { return id; }
    public String getCorreo() { return correo; }
    public Rol getRol() { return rol; }
    public String getClaveHash() { return claveHash; }
    public void setClaveHash(String claveHash) { this.claveHash = claveHash; }
    public boolean isSegundoFactorActivo() { return segundoFactorActivo; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int n) { this.intentosFallidos = n; }
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(Instant t) { this.bloqueadoHasta = t; }
    public Instant getConsentimientoEn() { return consentimientoEn; }
    public void setConsentimientoEn(Instant t) { this.consentimientoEn = t; }
    public Instant getBorradoSolicitadoEn() { return borradoSolicitadoEn; }
    public void setBorradoSolicitadoEn(Instant t) { this.borradoSolicitadoEn = t; }
}
