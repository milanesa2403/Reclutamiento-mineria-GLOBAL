package com.mineria.reclutamiento.modules.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(name = "correo", nullable = false, unique = true)
    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;

    @Column(name = "cuenta_activa", nullable = false)
    private boolean cuentaActiva = false;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    public Usuario() {
    }

    public Usuario(String correo, Rol rol) {
        this.correo = correo;
        this.rol = rol;
        this.cuentaActiva = true;
    }

    public Long getId() { return id; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public boolean isCuentaActiva() { return cuentaActiva; }
    public void setCuentaActiva(boolean cuentaActiva) { this.cuentaActiva = cuentaActiva; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
}
