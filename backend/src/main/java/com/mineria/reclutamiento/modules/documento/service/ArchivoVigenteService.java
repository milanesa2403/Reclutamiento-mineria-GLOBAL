package com.mineria.reclutamiento.modules.documento.service;

import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import com.mineria.reclutamiento.modules.documento.entity.Documento;
import com.mineria.reclutamiento.modules.documento.entity.TipoDocumento;
import com.mineria.reclutamiento.modules.documento.repository.DocumentoRepository;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ArchivoVigenteService {

    private final DocumentoService documentoService;
    private final DocumentoRepository documentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransactionTemplate transaccion;
    private final TransactionTemplate lectura;

    public ArchivoVigenteService(DocumentoService documentoService, DocumentoRepository documentoRepository,
                                 UsuarioRepository usuarioRepository, PlatformTransactionManager transactionManager) {
        this.documentoService = documentoService;
        this.documentoRepository = documentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.transaccion = new TransactionTemplate(transactionManager);
        this.lectura = new TransactionTemplate(transactionManager);
        this.lectura.setReadOnly(true);
    }

    public Documento reemplazar(Long idPropietario, Supplier<Collection<Long>> grupo, TipoDocumento tipo,
                                MultipartFile archivo, Set<String> formatos, Runnable verificacion) {
        DocumentoService.ArchivoValidado validado = documentoService.validar(archivo, formatos);
        String clave = documentoService.subirAlAlmacenamiento(idPropietario, tipo, validado);

        List<String> clavesAnteriores = new ArrayList<>();
        Documento nuevo;
        try {
            nuevo = transaccion.execute(status -> {
                verificacion.run();
                List<Documento> anteriores = documentoRepository
                        .dePropietarios(grupo.get(), tipo);
                Usuario propietario = usuarioRepository.getReferenceById(idPropietario);
                Documento registrado = documentoService.registrar(propietario, tipo, validado, clave);
                anteriores.forEach(d -> clavesAnteriores.add(d.getClaveAlmacenamiento()));
                documentoRepository.deleteAll(anteriores);
                return registrado;
            });
        } catch (RuntimeException e) {
            documentoService.borrarArchivo(clave);
            throw e;
        }
        clavesAnteriores.forEach(documentoService::borrarArchivo);
        return nuevo;
    }

    public Documento vigente(Collection<Long> grupo, TipoDocumento tipo) {
        return documentoRepository.vigente(grupo, tipo).orElse(null);
    }

    public Optional<String> urlVigente(Supplier<Collection<Long>> grupo, TipoDocumento tipo) {
        Documento documento = lectura.execute(status -> vigente(grupo.get(), tipo));
        return Optional.ofNullable(documento).map(documentoService::urlFirmada);
    }

    public String urlVigenteObligatoria(Supplier<Collection<Long>> grupo, TipoDocumento tipo, String mensaje) {
        return urlVigente(grupo, tipo).orElseThrow(() -> new ResourceNotFoundException(mensaje));
    }
}
