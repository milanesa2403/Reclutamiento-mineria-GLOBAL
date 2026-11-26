package com.mineria.reclutamiento.modules.documento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mineria.reclutamiento.core.exception.ForbiddenException;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import com.mineria.reclutamiento.modules.documento.entity.Documento;
import com.mineria.reclutamiento.modules.documento.entity.TipoDocumento;
import com.mineria.reclutamiento.modules.documento.repository.DocumentoRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArchivoVigenteServiceTest {

    @Mock DocumentoService documentoService;
    @Mock DocumentoRepository documentoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock PlatformTransactionManager txManager;

    ArchivoVigenteService service;
    final MockMultipartFile archivo = new MockMultipartFile("archivo", "cv.pdf", "application/pdf", "%PDF-1.7".getBytes());
    final DocumentoService.ArchivoValidado validado =
            new DocumentoService.ArchivoValidado("cv.pdf", DocumentoService.PDF, "pdf", "%PDF-1.7".getBytes(), "hash");

    @BeforeEach
    void setUp() {
        service = new ArchivoVigenteService(documentoService, documentoRepository, usuarioRepository, txManager);
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(documentoService.validar(any(), any())).thenReturn(validado);
        when(documentoService.subirAlAlmacenamiento(7L, TipoDocumento.CV, validado)).thenReturn("cv/7/nuevo.pdf");
        Usuario usuario = mock(Usuario.class);
        when(usuarioRepository.getReferenceById(7L)).thenReturn(usuario);
    }

    @Test
    void reemplazar_registraElNuevo_yBorraLosAnterioresDespuesDelCommit() {
        Documento anterior = mock(Documento.class);
        when(anterior.getClaveAlmacenamiento()).thenReturn("cv/7/viejo.pdf");
        when(documentoRepository.dePropietarios(List.of(7L), TipoDocumento.CV))
                .thenReturn(List.of(anterior));
        Documento nuevo = mock(Documento.class);
        when(documentoService.registrar(any(), eq(TipoDocumento.CV), eq(validado), eq("cv/7/nuevo.pdf"))).thenReturn(nuevo);

        Documento resultado = service.reemplazar(7L, () -> List.of(7L), TipoDocumento.CV, archivo,
                Set.of(DocumentoService.PDF), () -> { });

        assertThat(resultado).isSameAs(nuevo);
        verify(documentoRepository).deleteAll(List.of(anterior));
        verify(documentoService).borrarArchivo("cv/7/viejo.pdf");
        verify(documentoService, never()).borrarArchivo("cv/7/nuevo.pdf");
    }

    @Test
    void siLaVerificacionFalla_seBorraElArchivoRecienSubido_yNoSeTocaElAnterior() {
        Runnable yaNoAprobada = () -> {
            throw new ForbiddenException("ya no aprobada");
        };
        assertThatThrownBy(() -> service.reemplazar(7L, () -> List.of(7L), TipoDocumento.CV, archivo,
                Set.of(DocumentoService.PDF), yaNoAprobada))
                .isInstanceOf(ForbiddenException.class);

        verify(documentoService).borrarArchivo("cv/7/nuevo.pdf");
        verify(documentoRepository, never()).deleteAll(anyList());
        verify(documentoService, never()).registrar(any(), any(), any(), anyString());
    }
}
