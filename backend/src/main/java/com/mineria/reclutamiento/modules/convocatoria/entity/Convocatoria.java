package com.mineria.reclutamiento.modules.convocatoria.entity;

import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import com.mineria.reclutamiento.modules.ubigeo.entity.Ubigeo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "convocatoria")
public class Convocatoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_convocatoria")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empresa", nullable = false, updatable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_creador", updatable = false)
    private Usuario creador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_publicador")
    private Usuario publicador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoConvocatoria tipo;

    @Column(name = "puesto", nullable = false, length = 150)
    private String puesto;

    @Column(name = "campamento", nullable = false, length = 150)
    private String campamento;

    @Column(name = "turno", nullable = false, length = 50)
    private String turno;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "requisitos_obligatorios", nullable = false, columnDefinition = "TEXT")
    private String requisitosObligatorios;

    @Column(name = "requisitos_deseables", columnDefinition = "TEXT")
    private String requisitosDeseables;

    @Column(name = "experiencia_minima_anios")
    private Short experienciaMinimaAnios;

    @Column(name = "fecha_inicio_labores")
    private LocalDate fechaInicioLabores;

    @Column(name = "fecha_fin_labores")
    private LocalDate fechaFinLabores;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoConvocatoria estado = EstadoConvocatoria.BORRADOR;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ubigeo", nullable = false, columnDefinition = "bpchar(6)")
    private Ubigeo ubigeo;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "referencia", length = 255)
    private String referencia;

    @Column(name = "vacantes", nullable = false)
    private short vacantes = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_busqueda")
    private Busqueda busqueda;

    @OneToMany(mappedBy = "convocatoria", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<RequisitoCertificado> requisitosCertificado = new ArrayList<>();

    protected Convocatoria() {
    }

    public Convocatoria(Empresa empresa, Usuario creador, TipoConvocatoria tipo) {
        this.empresa = empresa;
        this.creador = creador;
        this.tipo = tipo;
    }

    public void publicar(Usuario publicador, LocalDateTime ahora) {
        if (estado != EstadoConvocatoria.BORRADOR) {
            throw new IllegalStateException("Solo se puede publicar una convocatoria en borrador");
        }
        this.estado = EstadoConvocatoria.PUBLICADA;
        this.publicador = publicador;
        this.fechaPublicacion = ahora;
    }

    public void cerrar() {
        if (estado != EstadoConvocatoria.PUBLICADA) {
            throw new IllegalStateException("Solo se puede cerrar una convocatoria publicada");
        }
        this.estado = EstadoConvocatoria.CERRADA;
    }

    public void reabrir() {
        if (estado != EstadoConvocatoria.CERRADA) {
            throw new IllegalStateException("Solo se puede reabrir una convocatoria cerrada");
        }
        this.estado = EstadoConvocatoria.PUBLICADA;
    }

    public Long getId() { return id; }
    public Empresa getEmpresa() { return empresa; }
    public Usuario getCreador() { return creador; }
    public Usuario getPublicador() { return publicador; }
    public TipoConvocatoria getTipo() { return tipo; }
    public void setTipo(TipoConvocatoria tipo) { this.tipo = tipo; }
    public String getPuesto() { return puesto; }
    public void setPuesto(String puesto) { this.puesto = puesto; }
    public String getCampamento() { return campamento; }
    public void setCampamento(String campamento) { this.campamento = campamento; }
    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getRequisitosObligatorios() { return requisitosObligatorios; }
    public void setRequisitosObligatorios(String requisitosObligatorios) { this.requisitosObligatorios = requisitosObligatorios; }
    public String getRequisitosDeseables() { return requisitosDeseables; }
    public void setRequisitosDeseables(String requisitosDeseables) { this.requisitosDeseables = requisitosDeseables; }
    public Short getExperienciaMinimaAnios() { return experienciaMinimaAnios; }
    public void setExperienciaMinimaAnios(Short experienciaMinimaAnios) { this.experienciaMinimaAnios = experienciaMinimaAnios; }
    public LocalDate getFechaInicioLabores() { return fechaInicioLabores; }
    public void setFechaInicioLabores(LocalDate fechaInicioLabores) { this.fechaInicioLabores = fechaInicioLabores; }
    public LocalDate getFechaFinLabores() { return fechaFinLabores; }
    public void setFechaFinLabores(LocalDate fechaFinLabores) { this.fechaFinLabores = fechaFinLabores; }
    public EstadoConvocatoria getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public LocalDate getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDate fechaCierre) { this.fechaCierre = fechaCierre; }
    public Ubigeo getUbigeo() { return ubigeo; }
    public void setUbigeo(Ubigeo ubigeo) { this.ubigeo = ubigeo; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public short getVacantes() { return vacantes; }
    public void setVacantes(short vacantes) { this.vacantes = vacantes; }
    public Busqueda getBusqueda() { return busqueda; }
    public void setBusqueda(Busqueda busqueda) { this.busqueda = busqueda; }
    public List<RequisitoCertificado> getRequisitosCertificado() { return requisitosCertificado; }

    public void reemplazarRequisitos(List<RequisitoCertificado> nuevos) {
        requisitosCertificado.clear();
        requisitosCertificado.addAll(nuevos);
    }
}
