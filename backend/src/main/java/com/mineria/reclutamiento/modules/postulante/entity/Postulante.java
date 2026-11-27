package com.mineria.reclutamiento.modules.postulante.entity;

import com.mineria.reclutamiento.modules.persona.entity.Persona;
import jakarta.persistence.*;

@Entity
@Table(name = "postulante")
public class Postulante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_postulante")
    private Long idPostulante;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_persona", nullable = false, unique = true)
    private Persona persona;

    @Column(name = "resumen_profesional", columnDefinition = "TEXT")
    private String resumenProfesional;

    @Column(name = "url_cv", columnDefinition = "TEXT")
    private String urlCv;

    @Column(name = "url_foto_dni", columnDefinition = "TEXT")
    private String urlFotoDni;

    @Column(name = "url_cul", columnDefinition = "TEXT")
    private String urlCul;

    @Column(name = "url_emo", columnDefinition = "TEXT")
    private String urlEmo;

    public Postulante() {}

    public Long getIdPostulante() { return idPostulante; }
    public void setIdPostulante(Long idPostulante) { this.idPostulante = idPostulante; }

    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }

    public String getResumenProfesional() { return resumenProfesional; }
    public void setResumenProfesional(String resumenProfesional) { this.resumenProfesional = resumenProfesional; }

    public String getUrlCv() { return urlCv; }
    public void setUrlCv(String urlCv) { this.urlCv = urlCv; }

    public String getUrlFotoDni() { return urlFotoDni; }
    public void setUrlFotoDni(String urlFotoDni) { this.urlFotoDni = urlFotoDni; }

    public String getUrlCul() { return urlCul; }
    public void setUrlCul(String urlCul) { this.urlCul = urlCul; }

    public String getUrlEmo() { return urlEmo; }
    public void setUrlEmo(String urlEmo) { this.urlEmo = urlEmo; }
}