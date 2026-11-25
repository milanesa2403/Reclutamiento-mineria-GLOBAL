package com.mineria.reclutamiento.modelo;

import jakarta.persistence.*;
import java.util.UUID;

/** Perfil del trabajador. Pais ISO 3166-1, idioma ISO 639-1. */
@Entity
@Table(name = "perfiles")
public class Perfil {
    @Id
    private UUID usuarioId;
    private String nombreCompleto;
    private String pais;
    private String idioma;
    private String especialidad;
    private int aniosExperiencia;

    protected Perfil() {}

    public Perfil(UUID usuarioId, String nombreCompleto, String pais, String idioma) {
        this.usuarioId = usuarioId;
        this.nombreCompleto = nombreCompleto;
        this.pais = pais;
        this.idioma = idioma;
    }

    public UUID getUsuarioId() { return usuarioId; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getPais() { return pais; }
    public String getIdioma() { return idioma; }
    public String getEspecialidad() { return especialidad; }
    public int getAniosExperiencia() { return aniosExperiencia; }
    public void setNombreCompleto(String v) { this.nombreCompleto = v; }
    public void setPais(String v) { this.pais = v; }
    public void setIdioma(String v) { this.idioma = v; }
    public void setEspecialidad(String v) { this.especialidad = v; }
    public void setAniosExperiencia(int v) { this.aniosExperiencia = v; }
}
