package com.mineria.reclutamiento.modules.postulante.service;

import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.documento.dto.UrlTemporalResponse;
import com.mineria.reclutamiento.modules.documento.service.ColaCvService;
import com.mineria.reclutamiento.modules.documento.service.DocumentoService;
import com.mineria.reclutamiento.modules.postulante.dto.response.DocumentosPostulanteResponse;
import com.mineria.reclutamiento.modules.postulante.entity.DocumentoPostulante;
import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import com.mineria.reclutamiento.modules.postulante.repository.PostulanteRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentosPostulanteService {

    private static final Pattern RUTA_PROPIA =
            Pattern.compile("^cvs/(cv|foto_dni|cul|emo)/(\\d+)/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(pdf|png|jpg)$");

    private final PostulanteService postulanteService;
    private final PostulanteRepository postulanteRepository;
    private final DocumentoService documentoService;
    private final ColaCvService colaCv;
    private final TransactionTemplate transaccion;
    private final TransactionTemplate lectura;

    public DocumentosPostulanteService(PostulanteService postulanteService, PostulanteRepository postulanteRepository,
                                       DocumentoService documentoService, ColaCvService colaCv,
                                       PlatformTransactionManager transactionManager) {
        this.postulanteService = postulanteService;
        this.colaCv = colaCv;
        this.postulanteRepository = postulanteRepository;
        this.documentoService = documentoService;
        this.transaccion = new TransactionTemplate(transactionManager);
        this.lectura = new TransactionTemplate(transactionManager);
        this.lectura.setReadOnly(true);
    }

    public DocumentosPostulanteResponse subir(String correo, DocumentoPostulante tipo, MultipartFile archivo) {
        Contexto ctx = lectura.execute(status -> contexto(correo));
        DocumentoService.ArchivoValidado validado = documentoService.validar(archivo, tipo.formatos());
        String clave = documentoService.subirAlAlmacenamiento(ctx.idUsuario(), tipo.tipoAlmacenamiento(), validado);

        String[] anterior = new String[1];
        DocumentosPostulanteResponse respuesta;
        try {
            respuesta = transaccion.execute(status -> {
                Postulante postulante = postulanteRepository.bloquear(ctx.idPostulante())
                        .orElseThrow(() -> new ResourceNotFoundException("Tu cuenta no tiene un perfil de postulante"));
                anterior[0] = tipo.leer(postulante);
                tipo.escribir(postulante, clave);
                return documentos(postulante, ctx.idUsuario());
            });
        } catch (RuntimeException e) {
            documentoService.borrarArchivo(clave);
            throw e;
        }
        if (esRutaPropia(anterior[0], tipo, ctx.idUsuario()) && !anterior[0].equals(clave)) {
            documentoService.borrarArchivo(anterior[0]);
        }
        if (tipo == DocumentoPostulante.CV) {
            colaCv.enviar(documentoService.bucket(), clave, "postulante-" + ctx.idPostulante());
        }
        return respuesta;
    }

    public UrlTemporalResponse url(String correo, DocumentoPostulante tipo) {
        String clave = lectura.execute(status -> {
            Postulante postulante = postulanteService.postulanteDe(correo);
            String valor = tipo.leer(postulante);
            return esRutaPropia(valor, tipo, postulante.getPersona().getUsuario().getId()) ? valor : null;
        });
        if (clave == null) {
            throw new ResourceNotFoundException("Aún no subiste " + tipo.nombre() + ".");
        }
        return new UrlTemporalResponse(documentoService.urlFirmada(clave), documentoService.segundosUrlFirmada());
    }


    public static DocumentosPostulanteResponse documentos(Postulante postulante, Long idUsuario) {
        return new DocumentosPostulanteResponse(
                item(postulante, DocumentoPostulante.CV, idUsuario),
                item(postulante, DocumentoPostulante.FOTO_DNI, idUsuario),
                item(postulante, DocumentoPostulante.CUL, idUsuario),
                item(postulante, DocumentoPostulante.EMO, idUsuario));
    }

    private static DocumentosPostulanteResponse.Documento item(Postulante p, DocumentoPostulante tipo, Long idUsuario) {
        String clave = tipo.leer(p);
        if (!esRutaPropia(clave, tipo, idUsuario)) {
            return null;
        }
        String extension = clave.substring(clave.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
        return new DocumentosPostulanteResponse.Documento(extension);
    }

    public static Optional<String> claveVigente(Postulante postulante, DocumentoPostulante tipo) {
        String clave = tipo.leer(postulante);
        Long idUsuario = postulante.getPersona().getUsuario().getId();
        return esRutaPropia(clave, tipo, idUsuario) ? Optional.of(clave) : Optional.empty();
    }

    static boolean esRutaPropia(String clave, DocumentoPostulante tipo, Long idUsuario) {
        if (clave == null || idUsuario == null) {
            return false;
        }
        var m = RUTA_PROPIA.matcher(clave);
        return m.matches()
                && m.group(1).equals(tipo.tipoAlmacenamiento().name().toLowerCase(Locale.ROOT))
                && m.group(2).equals(idUsuario.toString());
    }

    private record Contexto(Long idPostulante, Long idUsuario) {
    }

    private Contexto contexto(String correo) {
        Postulante postulante = postulanteService.postulanteDe(correo);
        return new Contexto(postulante.getIdPostulante(), postulante.getPersona().getUsuario().getId());
    }
}
