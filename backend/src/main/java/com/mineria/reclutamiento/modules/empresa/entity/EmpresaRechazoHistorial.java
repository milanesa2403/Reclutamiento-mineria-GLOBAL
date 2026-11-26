package com.mineria.reclutamiento.modules.empresa.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "empresa_rechazo_historial")
public class EmpresaRechazoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long id;

    @Column(name = "ruc", nullable = false, length = 20)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "fecha_rechazo", nullable = false)
    private LocalDateTime fechaRechazo;

    @CreationTimestamp
    @Column(name = "fecha_purga", nullable = false, updatable = false)
    private LocalDateTime fechaPurga;

    protected EmpresaRechazoHistorial() {
    }

    public EmpresaRechazoHistorial(String ruc, String razonSocial, String motivo, LocalDateTime fechaRechazo) {
        this.ruc = ruc;
        this.razonSocial = razonSocial;
        this.motivo = motivo;
        this.fechaRechazo = fechaRechazo;
    }

    public Long getId() { return id; }
    public String getRuc() { return ruc; }
    public String getRazonSocial() { return razonSocial; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getFechaRechazo() { return fechaRechazo; }
    public LocalDateTime getFechaPurga() { return fechaPurga; }
}
