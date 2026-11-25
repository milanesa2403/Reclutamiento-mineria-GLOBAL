package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.excepcion.AccesoDenegadoException;
import com.mineria.reclutamiento.excepcion.RecursoNoEncontradoException;
import com.mineria.reclutamiento.modelo.*;
import com.mineria.reclutamiento.repositorio.DocumentoRepositorio;
import com.mineria.reclutamiento.repositorio.EmpresaRepositorio;
import com.mineria.reclutamiento.repositorio.PostulacionRepositorio;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentoServicioImpl implements DocumentoServicio {
    private static final Set<String> EXTENSIONES = Set.of("pdf", "png", "jpg", "jpeg", "doc", "docx");
    private final DocumentoRepositorio documentos;
    private final PostulacionRepositorio postulaciones;
    private final EmpresaRepositorio empresas;
    private final AlmacenArchivos almacen;
    private final AuditoriaServicio auditoria;

    public DocumentoServicioImpl(DocumentoRepositorio documentos, PostulacionRepositorio postulaciones,
            EmpresaRepositorio empresas, AlmacenArchivos almacen, AuditoriaServicio auditoria) {
        this.documentos = documentos;
        this.postulaciones = postulaciones;
        this.empresas = empresas;
        this.almacen = almacen;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional
    public Documento subir(UUID propietarioId, TipoDocumento tipo, String nombre, byte[] contenido) {
        if (contenido == null || contenido.length == 0) throw new IllegalArgumentException("El archivo esta vacio");
        if (contenido.length > MAX_BYTES) throw new IllegalArgumentException("El archivo supera los 10 MB");
        String limpio = limpiarNombre(nombre);
        String ext = limpio.contains(".") ? limpio.substring(limpio.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!EXTENSIONES.contains(ext)) throw new IllegalArgumentException("Tipo de archivo no permitido (pdf, png, jpg, doc, docx)");

        String sha = calcularSha256(contenido);
        if (!almacen.existe(sha)) almacen.guardar(sha, contenido); // un solo original por contenido identico
        Documento d = new Documento(UUID.randomUUID(), propietarioId, tipo, limpio, sha, contenido.length);
        if (ext.equals("pdf") && !firmaPdfValida(contenido)) d.setEstado(EstadoDocumento.PENDIENTE_REVISION);
        auditoria.registrar(propietarioId, "SUBIDA_DOCUMENTO", "documento/" + d.getId(), true);
        return documentos.save(d);
    }

    @Override
    public Descarga descargar(UUID solicitanteId, Rol rol, UUID documentoId) {
        Documento d = documentos.findById(documentoId).orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado"));
        boolean permitido = rol == Rol.ADMINISTRADOR || d.getPropietarioId().equals(solicitanteId)
                || (rol == Rol.EMPRESA && empresas.findByUsuarioId(solicitanteId)
                        .map(e -> postulaciones.empresaPuedeVerDocumento(documentoId, e.getId())).orElse(false));
        auditoria.registrar(solicitanteId, "DESCARGA_DOCUMENTO", "documento/" + documentoId, permitido);
        if (!permitido) throw new AccesoDenegadoException("No tiene permiso para este archivo");
        return new Descarga(d.getNombreArchivo(), almacen.leer(d.getSha256()));
    }

    @Override
    public List<Documento> listar(UUID propietarioId) { return documentos.findByPropietarioId(propietarioId); }

    @Override
    public String calcularSha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean existeDuplicado(String sha256) { return documentos.existsBySha256(sha256); }

    private static boolean firmaPdfValida(byte[] c) {
        return c.length >= 4 && new String(c, 0, 4, StandardCharsets.ISO_8859_1).equals("%PDF");
    }

    private static String limpiarNombre(String nombre) {
        String n = nombre == null ? "" : nombre.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._-]", "_");
        if (n.isBlank()) throw new IllegalArgumentException("Nombre de archivo invalido");
        return n.length() > 120 ? n.substring(n.length() - 120) : n;
    }
}
