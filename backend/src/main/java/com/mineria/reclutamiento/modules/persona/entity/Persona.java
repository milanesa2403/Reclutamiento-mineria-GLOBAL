package com.mineria.reclutamiento.modules.persona.entity;

import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import jakarta.persistence.*;

@Entity
@Table(name = "persona")
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_persona")
    private Long idPersona;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "pais", nullable = false, length = 2)
    private String pais = "PE";

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumentoIdentidad tipoDocumento = TipoDocumentoIdentidad.DNI;

    @Column(name = "dni", nullable = false, length = 20)
    private String dni;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "direccion", length = 255)
    private String direccion;

    public Persona() {
    }

    public Persona(Long idPersona, Usuario usuario, String dni, String nombres, String apellidos, String telefono, String direccion) {
        this.idPersona = idPersona;
        this.usuario = usuario;
        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.direccion = direccion;
    }

    public Long getIdPersona() { return idPersona; }
    public void setIdPersona(Long idPersona) { this.idPersona = idPersona; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    public TipoDocumentoIdentidad getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(TipoDocumentoIdentidad tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}