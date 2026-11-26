package com.mineria.reclutamiento.modules.empresa.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "actividad_empresa")
public class ActividadEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actividad")
    private Long id;

    @Column(name = "id_empresa")
    private Long idEmpresa;

    @Column(name = "ruc", nullable = false, length = 20)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoActividadEmpresa tipo;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "realizado_por", length = 150)
    private String realizadoPor;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    protected ActividadEmpresa() {
    }

    public ActividadEmpresa(Long idEmpresa, String ruc, String razonSocial, TipoActividadEmpresa tipo,
                            String motivo, String realizadoPor) {
        this.idEmpresa = idEmpresa;
        this.ruc = ruc;
        this.razonSocial = razonSocial;
        this.tipo = tipo;
        this.motivo = motivo;
        this.realizadoPor = realizadoPor;
        this.fecha = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public Long getIdEmpresa() { return idEmpresa; }
    public String getRuc() { return ruc; }
    public String getRazonSocial() { return razonSocial; }
    public TipoActividadEmpresa getTipo() { return tipo; }
    public String getMotivo() { return motivo; }
    public String getRealizadoPor() { return realizadoPor; }
    public LocalDateTime getFecha() { return fecha; }
}