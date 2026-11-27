package com.mineria.reclutamiento.modules.empresa.entity;

import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.documento.entity.Documento;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "empresa")
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa")
    private Long id;

    @Column(name = "pais", nullable = false, length = 2)
    private String pais = "PE";

    @Column(name = "ruc", nullable = false, length = 20)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "correo_corporativo", nullable = false, length = 255)
    private String correoCorporativo;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "imagen_perfil", length = 500)
    private String imagenPerfil;

    @Column(name = "banner", length = 500)
    private String banner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sector_minero", nullable = false)
    private SectorMinero sectorMinero;


    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoEmpresa estado = EstadoEmpresa.PENDIENTE_DOCUMENTO;

    @Column(name = "motivo_revision", length = 500)
    private String motivoRevision;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin_revisor")
    private Usuario adminRevisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_documento_acreditacion")
    private Documento documentoAcreditacion;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    public Empresa() {
    }

    public Empresa(String ruc, String razonSocial, String direccion, String telefono, String correoCorporativo, SectorMinero sectorMinero) {
        this.ruc = ruc;
        this.razonSocial = razonSocial;
        this.direccion = direccion;
        this.telefono = telefono;
        this.correoCorporativo = correoCorporativo;
        this.sectorMinero = sectorMinero;
    }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getImagenPerfil() { return imagenPerfil; }
    public void setImagenPerfil(String imagenPerfil) { this.imagenPerfil = imagenPerfil; }
    public String getBanner() { return banner; }
    public void setBanner(String banner) { this.banner = banner; }
    public Long getId() { return id; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreoCorporativo() { return correoCorporativo; }
    public void setCorreoCorporativo(String correoCorporativo) { this.correoCorporativo = correoCorporativo; }
    public SectorMinero getSectorMinero() { return sectorMinero; }
    public void setSectorMinero(SectorMinero sectorMinero) { this.sectorMinero = sectorMinero; }
    public EstadoEmpresa getEstado() { return estado; }
    public void setEstado(EstadoEmpresa estado) { this.estado = estado; }
    public String getMotivoRevision() { return motivoRevision; }
    public void setMotivoRevision(String motivoRevision) { this.motivoRevision = motivoRevision; }
    public LocalDateTime getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDateTime fechaRevision) { this.fechaRevision = fechaRevision; }
    public Usuario getAdminRevisor() { return adminRevisor; }
    public void setAdminRevisor(Usuario adminRevisor) { this.adminRevisor = adminRevisor; }
    public Documento getDocumentoAcreditacion() { return documentoAcreditacion; }
    public void setDocumentoAcreditacion(Documento documentoAcreditacion) { this.documentoAcreditacion = documentoAcreditacion; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public boolean estaAprobada() { return estado == EstadoEmpresa.APROBADA; }
}