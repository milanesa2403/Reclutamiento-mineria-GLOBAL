package com.mineria.reclutamiento.modules.empresa.entity;

import com.mineria.reclutamiento.modules.persona.entity.Persona;
import jakarta.persistence.*;

@Entity
@Table(name = "representante_empresa")
public class Representante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_representante")
    private Long idRepresentante;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_persona", nullable = false, unique = true)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    @Column(name = "cargo", length = 100)
    private String cargo;

    public Representante() {
    }

    public Representante(Long idRepresentante, Persona persona, Empresa empresa, String cargo) {
        this.idRepresentante = idRepresentante;
        this.persona = persona;
        this.empresa = empresa;
        this.cargo = cargo;
    }

    public Long getIdRepresentante() { return idRepresentante; }
    public void setIdRepresentante(Long idRepresentante) { this.idRepresentante = idRepresentante; }

    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) { this.persona = persona; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa;}

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
}