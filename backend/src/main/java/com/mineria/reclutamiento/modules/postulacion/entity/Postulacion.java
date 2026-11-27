package com.mineria.reclutamiento.modules.postulacion.entity;

import com.mineria.reclutamiento.modules.convocatoria.entity.Convocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.RequisitoCertificado;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import com.mineria.reclutamiento.modules.postulante.entity.Certificacion;
import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import com.mineria.reclutamiento.core.exception.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "postulacion")
public class Postulacion {

    public static final int DIAS_PARA_CONFIRMAR = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_postulacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_convocatoria", nullable = false, updatable = false)
    private Convocatoria convocatoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_postulante", nullable = false, updatable = false)
    private Postulante postulante;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPostulacion estado = EstadoPostulacion.POSTULADO;

    @CreationTimestamp
    @Column(name = "fecha_postulacion", nullable = false, updatable = false)
    private LocalDateTime fechaPostulacion;

    @Column(name = "fecha_cambio_estado")
    private LocalDateTime fechaCambioEstado;

    @Column(name = "fecha_aceptacion")
    private LocalDateTime fechaAceptacion;

    @Column(name = "motivo_retiro", length = 60)
    private String motivoRetiro;

    @Column(name = "visto", nullable = false)
    private boolean visto = true;

    @Embedded
    private Entrevista entrevista;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "postulacion_certificacion",
            joinColumns = @JoinColumn(name = "id_postulacion"),
            inverseJoinColumns = @JoinColumn(name = "id_certificacion"))
    private Set<Certificacion> certificaciones = new HashSet<>();

    @OneToMany(mappedBy = "postulacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostulacionRequisito> requisitosCubiertos = new ArrayList<>();

    protected Postulacion() {
    }

    public Postulacion(Convocatoria convocatoria, Postulante postulante, Set<Certificacion> certificaciones) {
        this.convocatoria = convocatoria;
        this.postulante = postulante;
        if (certificaciones != null) {
            this.certificaciones.addAll(certificaciones);
        }
    }

    public void cambiarEstado(EstadoPostulacion nuevo, LocalDateTime ahora) {
        if (nuevo == null || nuevo == EstadoPostulacion.POSTULADO) {
            throw new IllegalArgumentException("Estado de revisión no válido");
        }
        if (estado != EstadoPostulacion.CONFIRMADO || nuevo != EstadoPostulacion.DESCARTADO) {
            exigirActiva("Esta postulación ya no se puede cambiar.");
        }
        this.estado = nuevo;
        this.fechaCambioEstado = ahora;
        this.fechaAceptacion = nuevo == EstadoPostulacion.ACEPTADO ? ahora : null;
        this.visto = nuevo == EstadoPostulacion.EN_REVISION;
    }

    public void citar(Entrevista entrevista, LocalDateTime ahora) {
        if (!estado.activo() || estado == EstadoPostulacion.ACEPTADO) {
            throw new BadRequestException("Solo puedes citar a un candidato en Nuevo, En revisión o Entrevista.");
        }
        entrevista.validar(ahora);
        this.entrevista = entrevista;
        this.estado = EstadoPostulacion.ENTREVISTA;
        this.fechaCambioEstado = ahora;
        this.visto = false;
    }

    public void confirmar(LocalDateTime ahora) {
        if (estado != EstadoPostulacion.ACEPTADO || (fechaAceptacion != null && ahora.isAfter(getFechaLimite()))) {
            throw new BadRequestException("Esta oferta ya no se puede confirmar.");
        }
        this.estado = EstadoPostulacion.CONFIRMADO;
        this.fechaCambioEstado = ahora;
    }

    public void retirar(String motivo, LocalDateTime ahora) {
        exigirActiva("Esta postulación ya no se puede retirar.");
        this.estado = EstadoPostulacion.RETIRADO;
        this.motivoRetiro = motivo;
        this.fechaCambioEstado = ahora;
    }

    private void exigirActiva(String mensaje) {
        if (!estado.activo()) {
            throw new BadRequestException(mensaje);
        }
    }

    public LocalDateTime getFechaLimite() {
        return fechaAceptacion == null ? null : fechaAceptacion.plusDays(DIAS_PARA_CONFIRMAR);
    }

    public void marcarVisto() { this.visto = true; }
    public String getMotivoRetiro() { return motivoRetiro; }
    public boolean isVisto() { return visto; }
    public Entrevista getEntrevista() { return entrevista; }

    public Long getId() { return id; }
    public Convocatoria getConvocatoria() { return convocatoria; }
    public Postulante getPostulante() { return postulante; }
    public EstadoPostulacion getEstado() { return estado; }
    public LocalDateTime getFechaPostulacion() { return fechaPostulacion; }
    public LocalDateTime getFechaCambioEstado() { return fechaCambioEstado; }
    public Set<Certificacion> getCertificaciones() { return certificaciones; }
    public List<PostulacionRequisito> getRequisitosCubiertos() { return requisitosCubiertos; }

    public void cubrirRequisito(RequisitoCertificado requisito, Certificacion certificacion) {
        requisitosCubiertos.add(new PostulacionRequisito(this, requisito, certificacion));
        certificaciones.add(certificacion);
    }
}
