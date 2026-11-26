package com.mineria.reclutamiento.modules.documento.entity;

import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "documento")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario propietario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoDocumento tipo;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "clave_almacenamiento", nullable = false, unique = true, length = 500)
    private String claveAlmacenamiento;

    @CreationTimestamp
    @Column(name = "fecha_subida", nullable = false, updatable = false)
    private LocalDateTime fechaSubida;

    protected Documento() {
    }

    public Documento(Usuario propietario, TipoDocumento tipo, String nombreOriginal, String tipoMime,
                     long tamanoBytes, String hashSha256, String claveAlmacenamiento) {
        this.propietario = propietario;
        this.tipo = tipo;
        this.nombreOriginal = nombreOriginal;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamanoBytes;
        this.hashSha256 = hashSha256;
        this.claveAlmacenamiento = claveAlmacenamiento;
    }

    public Long getId() { return id; }
    public Usuario getPropietario() { return propietario; }
    public TipoDocumento getTipo() { return tipo; }
    public String getNombreOriginal() { return nombreOriginal; }
    public String getTipoMime() { return tipoMime; }
    public long getTamanoBytes() { return tamanoBytes; }
    public String getHashSha256() { return hashSha256; }
    public String getClaveAlmacenamiento() { return claveAlmacenamiento; }
    public LocalDateTime getFechaSubida() { return fechaSubida; }
}
