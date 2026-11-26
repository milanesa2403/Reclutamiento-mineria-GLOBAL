package com.mineria.reclutamiento.modules.postulante.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.auth.entity.Rol;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.documento.entity.TipoDocumento;
import com.mineria.reclutamiento.modules.documento.service.ColaCvService;
import com.mineria.reclutamiento.modules.documento.service.DocumentoService;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.postulante.dto.response.DocumentosPostulanteResponse;
import com.mineria.reclutamiento.modules.postulante.entity.DocumentoPostulante;
import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import com.mineria.reclutamiento.modules.postulante.repository.PostulanteRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
class DocumentosPostulanteServiceTest {

    static final String CORREO = "post@prueba.pe";
    static final String UUID_1 = "11111111-1111-1111-1111-111111111111";
    static final String UUID_2 = "22222222-2222-2222-2222-222222222222";

    @Mock PostulanteService postulanteService;
    @Mock PostulanteRepository postulanteRepository;
    @Mock DocumentoService documentoService;
    @Mock ColaCvService colaCv;
    @Mock PlatformTransactionManager txManager;

    DocumentosPostulanteService service;
    Postulante postulante;
    final MockMultipartFile archivo = new MockMultipartFile("archivo", "a.pdf", "application/pdf", "%PDF-1.7".getBytes());
    final DocumentoService.ArchivoValidado validado =
            new DocumentoService.ArchivoValidado("a.pdf", DocumentoService.PDF, "pdf", "%PDF-1.7".getBytes(), "h");

    @BeforeEach
    void setUp() {
        service = new DocumentosPostulanteService(postulanteService, postulanteRepository, documentoService, colaCv,
                txManager);
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        Usuario usuario = new Usuario(CORREO, Rol.POSTULANTE);
        ReflectionTestUtils.setField(usuario, "id", 7L);
        postulante = new Postulante();
        ReflectionTestUtils.setField(postulante, "idPostulante", 3L);
        postulante.setPersona(new Persona(10L, usuario, "12345678", "ANA", "RUIZ", "987654321", null));
        when(postulanteService.postulanteDe(CORREO)).thenReturn(postulante);
        when(postulanteRepository.bloquear(3L)).thenReturn(Optional.of(postulante));
        when(documentoService.validar(any(), any())).thenReturn(validado);
    }

    private static String ruta(DocumentoPostulante tipo, long idUsuario, String uuid) {
        return "cvs/" + tipo.tipoAlmacenamiento().name().toLowerCase() + "/" + idUsuario + "/" + uuid + ".pdf";
    }

    @ParameterizedTest
    @EnumSource(DocumentoPostulante.class)
    void reemplazo_guardaLaNuevaRuta_yBorraElArchivoAnterior(DocumentoPostulante tipo) {
        String anterior = ruta(tipo, 7, UUID_1);
        String nueva = ruta(tipo, 7, UUID_2);
        tipo.escribir(postulante, anterior);
        when(documentoService.subirAlAlmacenamiento(7L, tipo.tipoAlmacenamiento(), validado)).thenReturn(nueva);

        DocumentosPostulanteResponse res = service.subir(CORREO, tipo, archivo);

        assertThat(tipo.leer(postulante)).isEqualTo(nueva);
        verify(documentoService).validar(archivo, tipo.formatos());
        verify(documentoService).borrarArchivo(anterior);
        verify(documentoService, never()).borrarArchivo(nueva);
        assertThat(res.toString()).doesNotContain(nueva);
    }

    @Test
    void soloElCv_seEnviaALaColaDeRevision() {
        String nueva = ruta(DocumentoPostulante.CV, 7, UUID_2);
        when(documentoService.subirAlAlmacenamiento(7L, TipoDocumento.CV, validado)).thenReturn(nueva);
        when(documentoService.bucket()).thenReturn("bucket-cvs");

        service.subir(CORREO, DocumentoPostulante.CV, archivo);

        verify(colaCv).enviar("bucket-cvs", nueva, "postulante-3");
    }

    @Test
    void otrosDocumentos_noSeEnvianALaCola() {
        when(documentoService.subirAlAlmacenamiento(any(), any(), any())).thenReturn(ruta(DocumentoPostulante.EMO, 7, UUID_2));

        service.subir(CORREO, DocumentoPostulante.EMO, archivo);

        verify(colaCv, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void primeraSubida_noBorraNada() {
        when(documentoService.subirAlAlmacenamiento(7L, TipoDocumento.CV, validado)).thenReturn(ruta(DocumentoPostulante.CV, 7, UUID_2));
        DocumentosPostulanteResponse res = service.subir(CORREO, DocumentoPostulante.CV, archivo);
        assertThat(res.cv().formato()).isEqualTo("PDF");
        assertThat(res.fotoDni()).isNull();
        verify(documentoService, never()).borrarArchivo(anyString());
    }

    @Test
    void valorAntiguo_urlPublica_oRutaDeOtroUsuario_noSeBorraNuncaDelBucket() {
        postulante.setUrlCv("https://x.supabase.co/storage/v1/object/public/documentos-postulantes/52/cv_1.pdf");
        postulante.setUrlCul(ruta(DocumentoPostulante.CUL, 99, UUID_1));
        when(documentoService.subirAlAlmacenamiento(any(), any(), any())).thenReturn(ruta(DocumentoPostulante.CV, 7, UUID_2));

        service.subir(CORREO, DocumentoPostulante.CV, archivo);

        verify(documentoService, never()).borrarArchivo(anyString());
        assertThat(DocumentosPostulanteService.documentos(postulante, 7L).cul()).isNull();
    }

    @Test
    void siFallaLaBd_seBorraElArchivoRecienSubido_yElAnteriorSeConserva() {
        String anterior = ruta(DocumentoPostulante.EMO, 7, UUID_1);
        postulante.setUrlEmo(anterior);
        String nueva = ruta(DocumentoPostulante.EMO, 7, UUID_2);
        when(documentoService.subirAlAlmacenamiento(any(), any(), any())).thenReturn(nueva);
        when(postulanteRepository.bloquear(3L)).thenThrow(new IllegalStateException("BD caída"));

        assertThatThrownBy(() -> service.subir(CORREO, DocumentoPostulante.EMO, archivo))
                .isInstanceOf(IllegalStateException.class);
        verify(documentoService).borrarArchivo(nueva);
        verify(documentoService, never()).borrarArchivo(anterior);
    }

    @Test
    void archivoInvalido_noSubeNada() {
        when(documentoService.validar(any(), any())).thenThrow(new BadRequestException("Formato no permitido"));
        assertThatThrownBy(() -> service.subir(CORREO, DocumentoPostulante.CUL, archivo))
                .isInstanceOf(BadRequestException.class);
        verify(documentoService, never()).subirAlAlmacenamiento(any(), any(), any());
    }

    @Test
    void url_firmada_soloSiHayDocumentoPropio() {
        assertThatThrownBy(() -> service.url(CORREO, DocumentoPostulante.FOTO_DNI))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("el DNI");

        String clave = ruta(DocumentoPostulante.FOTO_DNI, 7, UUID_1);
        postulante.setUrlFotoDni(clave);
        when(documentoService.urlFirmada(clave)).thenReturn("https://x/sign/...");
        when(documentoService.segundosUrlFirmada()).thenReturn(300);
        assertThat(service.url(CORREO, DocumentoPostulante.FOTO_DNI).expiraEnSegundos()).isEqualTo(300);
        verify(documentoService).urlFirmada(clave);
        verify(documentoService, never()).urlFirmada(eq(ruta(DocumentoPostulante.FOTO_DNI, 99, UUID_1)));
    }
}
