package com.mineria.reclutamiento.modules.postulante.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.auth.entity.Rol;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.documento.entity.Documento;
import com.mineria.reclutamiento.modules.documento.entity.TipoDocumento;
import com.mineria.reclutamiento.modules.documento.repository.DocumentoRepository;
import com.mineria.reclutamiento.modules.documento.service.DocumentoService;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.postulante.entity.Certificacion;
import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import com.mineria.reclutamiento.modules.postulante.repository.CertificacionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CertificadoArchivoServiceTest {

    static final String CORREO = "post@prueba.pe";

    @Mock PostulanteService postulanteService;
    @Mock CertificacionRepository certificacionRepository;
    @Mock DocumentoRepository documentoRepository;
    @Mock DocumentoService documentoService;
    @Mock PlatformTransactionManager txManager;

    CertificadoArchivoService service;
    Postulante postulante;
    Certificacion cert;
    final MockMultipartFile archivo = new MockMultipartFile("archivo", "sctr.pdf", "application/pdf", "%PDF-1.7".getBytes());
    final DocumentoService.ArchivoValidado validado =
            new DocumentoService.ArchivoValidado("sctr.pdf", DocumentoService.PDF, "pdf", "%PDF-1.7".getBytes(), "h");

    @BeforeEach
    void setUp() {
        service = new CertificadoArchivoService(postulanteService, certificacionRepository, documentoRepository,
                documentoService, txManager);
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        Usuario usuario = new Usuario(CORREO, Rol.POSTULANTE);
        ReflectionTestUtils.setField(usuario, "id", 7L);
        postulante = new Postulante();
        ReflectionTestUtils.setField(postulante, "idPostulante", 3L);
        postulante.setPersona(new Persona(10L, usuario, "12345678", "ANA", "RUIZ", "987654321", null));
        cert = new Certificacion(postulante, null, "SCTR", "Rimac", null);
        when(postulanteService.postulanteDe(CORREO)).thenReturn(postulante);
        when(certificacionRepository.findByIdAndPostulante_IdPostulante(5L, 3L)).thenReturn(Optional.of(cert));
        when(certificacionRepository.bloquearDelPostulante(5L, 3L)).thenReturn(Optional.of(cert));
        when(documentoService.validar(any(), any())).thenReturn(validado);
        when(documentoService.subirAlAlmacenamiento(7L, TipoDocumento.CERTIFICADO, validado)).thenReturn("certificado/7/nuevo.pdf");
    }

    @Test
    void certificacionAjenaOInexistente_404_sinSubirNada() {
        assertThatThrownBy(() -> service.subir(CORREO, 999L, archivo)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.url(CORREO, 999L)).isInstanceOf(ResourceNotFoundException.class);
        verify(documentoService, never()).subirAlAlmacenamiento(any(), any(), any());
    }

    @Test
    void soloPdf() {
        service.subir(CORREO, 5L, archivo);
        verify(documentoService).validar(archivo, CertificadoArchivoService.FORMATOS_CERTIFICADO);
        assertThat(CertificadoArchivoService.FORMATOS_CERTIFICADO).containsExactly(DocumentoService.PDF);
    }

    @Test
    void reemplazo_registraElNuevo_borraLaFilaYElArchivoAnterior() {
        Documento anterior = mock(Documento.class);
        when(anterior.getClaveAlmacenamiento()).thenReturn("certificado/7/viejo.pdf");
        cert.setDocumento(anterior);
        Documento nuevo = mock(Documento.class);
        when(documentoService.registrar(any(), eq(TipoDocumento.CERTIFICADO), eq(validado), eq("certificado/7/nuevo.pdf")))
                .thenReturn(nuevo);

        service.subir(CORREO, 5L, archivo);

        assertThat(cert.getDocumento()).isSameAs(nuevo);
        verify(documentoRepository).delete(anterior);
        verify(documentoService).borrarArchivo("certificado/7/viejo.pdf");
        verify(documentoService, never()).borrarArchivo("certificado/7/nuevo.pdf");
    }

    @Test
    void siLaCertificacionSeEliminoMientrasSeSubia_seBorraElArchivoNuevo() {
        when(certificacionRepository.bloquearDelPostulante(5L, 3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.subir(CORREO, 5L, archivo)).isInstanceOf(ResourceNotFoundException.class);
        verify(documentoService).borrarArchivo("certificado/7/nuevo.pdf");
    }

    @Test
    void url_404SiNoTienePdf() {
        assertThatThrownBy(() -> service.url(CORREO, 5L)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("PDF");
        verify(documentoService, never()).urlFirmada(anyString());
    }
}
