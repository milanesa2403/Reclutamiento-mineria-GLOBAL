package com.mineria.reclutamiento.modelo;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Guarda inicios de sesion, registros, accesos a datos personales e intentos 403 (con IP y hora). */
@Entity
@Table(name = "auditoria", indexes = @Index(name = "idx_auditoria_ip_fecha", columnList = "ip,fecha"))
public class RegistroAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private UUID usuarioId;
    private String accion;
    private String recurso;
    private boolean permitido;
    private String ip;
    private Instant fecha = Instant.now();

    protected RegistroAuditoria() {}

    public RegistroAuditoria(UUID usuarioId, String accion, String recurso, boolean permitido, String ip) {
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.recurso = recurso;
        this.permitido = permitido;
        this.ip = ip;
    }

    public Long getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getAccion() { return accion; }
    public String getRecurso() { return recurso; }
    public boolean isPermitido() { return permitido; }
    public String getIp() { return ip; }
    public Instant getFecha() { return fecha; }
}
