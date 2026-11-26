package com.mineria.reclutamiento.modules.documento.service;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.documento.entity.Documento;
import com.mineria.reclutamiento.modules.documento.entity.TipoDocumento;
import com.mineria.reclutamiento.modules.documento.repository.DocumentoRepository;
import com.mineria.reclutamiento.modules.documento.storage.AlmacenamientoArchivos;
import com.mineria.reclutamiento.core.config.AwsProperties;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final String PDF = "application/pdf";
    public static final String PNG = "image/png";
    public static final String JPEG = "image/jpeg";

    private final AlmacenamientoArchivos almacenamiento;
    private final DocumentoRepository documentoRepository;
    private final AwsProperties props;

    public DocumentoService(AlmacenamientoArchivos almacenamiento, DocumentoRepository documentoRepository,
                            AwsProperties props) {
        this.almacenamiento = almacenamiento;
        this.documentoRepository = documentoRepository;
        this.props = props;
    }

    public record ArchivoValidado(String nombreOriginal, String tipoMime, String extension, byte[] contenido,
                                  String hashSha256) {
    }

    public ArchivoValidado validar(MultipartFile archivo, Set<String> tiposPermitidos) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("Debes adjuntar un archivo.");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new BadRequestException("El archivo supera el tamaño máximo de 5 MB.");
        }
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("No se pudo leer el archivo. Intenta nuevamente.");
        }
        String tipoMime = detectarTipo(contenido);
        if (tipoMime == null || !tiposPermitidos.contains(tipoMime)) {
            throw new BadRequestException("Formato de archivo no permitido. Formatos aceptados: "
                    + String.join(", ", tiposPermitidos.stream().map(DocumentoService::nombreFormato).sorted().toList())
                    + ".");
        }
        return new ArchivoValidado(nombreSeguro(archivo.getOriginalFilename()), tipoMime, extension(tipoMime),
                contenido, sha256(contenido));
    }

    public String subirAlAlmacenamiento(Long idUsuario, TipoDocumento tipo, ArchivoValidado archivo) {
        String clave = props.prefijo() + "/" + tipo.name().toLowerCase(Locale.ROOT) + "/" + idUsuario + "/"
                + UUID.randomUUID() + "." + archivo.extension();
        almacenamiento.subir(clave, archivo.contenido(), archivo.tipoMime());
        return clave;
    }

    public Documento registrar(Usuario propietario, TipoDocumento tipo, ArchivoValidado archivo, String clave) {
        return documentoRepository.save(new Documento(propietario, tipo, archivo.nombreOriginal(), archivo.tipoMime(),
                archivo.contenido().length, archivo.hashSha256(), clave));
    }

    public void borrarArchivo(String clave) {
        try {
            almacenamiento.eliminar(clave);
        } catch (RuntimeException e) {
            log.warn("No se pudo eliminar un archivo huérfano del almacenamiento ({}): queda para limpieza manual",
                    e.getClass().getSimpleName());
        }
    }

    public void eliminarArchivo(String clave) {
        almacenamiento.eliminar(clave);
    }

    public String urlFirmada(Documento documento) {
        return urlFirmada(documento.getClaveAlmacenamiento());
    }

    public String urlFirmada(String clave) {
        return almacenamiento.urlFirmada(clave, props.urlFirmadaSegundos());
    }

    public String bucket() {
        return almacenamiento.bucket();
    }

    public int segundosUrlFirmada() {
        return props.urlFirmadaSegundos();
    }


    static String detectarTipo(byte[] b) {
        if (b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') {
            return PDF;
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return PNG;
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return JPEG;
        }
        return null;
    }

    private static String extension(String tipoMime) {
        return switch (tipoMime) {
            case PDF -> "pdf";
            case PNG -> "png";
            case JPEG -> "jpg";
            default -> throw new IllegalArgumentException("Tipo no soportado");
        };
    }

    private static String nombreFormato(String tipoMime) {
        return switch (tipoMime) {
            case PDF -> "PDF";
            case PNG -> "PNG";
            case JPEG -> "JPG";
            default -> tipoMime;
        };
    }

    static String nombreSeguro(String original) {
        if (original == null || original.isBlank()) {
            return "archivo";
        }
        String nombre = original.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1);
        nombre = nombre.replaceAll("[\\p{Cntrl}]", "").trim();
        if (nombre.isEmpty()) {
            return "archivo";
        }
        return nombre.length() > 255 ? nombre.substring(nombre.length() - 255) : nombre;
    }

    private static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
