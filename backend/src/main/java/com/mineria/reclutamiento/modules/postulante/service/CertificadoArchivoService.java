package com.mineria.reclutamiento.modules.postulante.service;

import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.documento.dto.DocumentoResponse;
import com.mineria.reclutamiento.modules.documento.dto.UrlTemporalResponse;
import com.mineria.reclutamiento.modules.documento.entity.Documento;
import com.mineria.reclutamiento.modules.documento.entity.TipoDocumento;
import com.mineria.reclutamiento.modules.documento.repository.DocumentoRepository;
import com.mineria.reclutamiento.modules.documento.service.DocumentoService;
import com.mineria.reclutamiento.modules.postulante.entity.Certificacion;
import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import com.mineria.reclutamiento.modules.postulante.repository.CertificacionRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CertificadoArchivoService {

    static final Set<String> FORMATOS_CERTIFICADO = Set.of(DocumentoService.PDF);
    private static final String NO_ENCONTRADA = "Certificación no encontrada";

    private final PostulanteService postulanteService;
    private final CertificacionRepository certificacionRepository;
    private final DocumentoRepository documentoRepository;
    private final DocumentoService documentoService;
    private final TransactionTemplate transaccion;
    private final TransactionTemplate lectura;

    public CertificadoArchivoService(PostulanteService postulanteService, CertificacionRepository certificacionRepository,
                                     DocumentoRepository documentoRepository, DocumentoService documentoService,
                                     PlatformTransactionManager transactionManager) {
        this.postulanteService = postulanteService;
        this.certificacionRepository = certificacionRepository;
        this.documentoRepository = documentoRepository;
        this.documentoService = documentoService;
        this.transaccion = new TransactionTemplate(transactionManager);
        this.lectura = new TransactionTemplate(transactionManager);
        this.lectura.setReadOnly(true);
    }

    public DocumentoResponse subir(String correo, Long idCertificacion, MultipartFile archivo) {
        Contexto ctx = lectura.execute(status -> {
            Postulante p = postulanteService.postulanteDe(correo);
            certificacionRepository.findByIdAndPostulante_IdPostulante(idCertificacion, p.getIdPostulante())
                    .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADA));
            return new Contexto(p.getIdPostulante(), p.getPersona().getUsuario().getId());
        });
        DocumentoService.ArchivoValidado validado = documentoService.validar(archivo, FORMATOS_CERTIFICADO);
        String clave = documentoService.subirAlAlmacenamiento(ctx.idUsuario(), TipoDocumento.CERTIFICADO, validado);

        String[] claveAnterior = new String[1];
        DocumentoResponse respuesta;
        try {
            respuesta = transaccion.execute(status -> {
                Certificacion cert = certificacionRepository.bloquearDelPostulante(idCertificacion, ctx.idPostulante())
                        .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADA));
                Postulante postulante = cert.getPostulante();
                Documento anterior = cert.getDocumento();
                Documento nuevo = documentoService.registrar(postulante.getPersona().getUsuario(),
                        TipoDocumento.CERTIFICADO, validado, clave);
                cert.setDocumento(nuevo);
                if (anterior != null) {
                    claveAnterior[0] = anterior.getClaveAlmacenamiento();
                    certificacionRepository.saveAndFlush(cert);
                    documentoRepository.delete(anterior);
                }
                return DocumentoResponse.de(nuevo);
            });
        } catch (RuntimeException e) {
            documentoService.borrarArchivo(clave);
            throw e;
        }
        if (claveAnterior[0] != null) {
            documentoService.borrarArchivo(claveAnterior[0]);
        }
        return respuesta;
    }

    public UrlTemporalResponse url(String correo, Long idCertificacion) {
        String clave = lectura.execute(status -> {
            Postulante p = postulanteService.postulanteDe(correo);
            Certificacion cert = certificacionRepository
                    .findByIdAndPostulante_IdPostulante(idCertificacion, p.getIdPostulante())
                    .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADA));
            if (cert.getDocumento() == null) {
                throw new ResourceNotFoundException("Esta certificación aún no tiene su PDF.");
            }
            return cert.getDocumento().getClaveAlmacenamiento();
        });
        return new UrlTemporalResponse(documentoService.urlFirmada(clave), documentoService.segundosUrlFirmada());
    }

    private record Contexto(Long idPostulante, Long idUsuario) {
    }
}
