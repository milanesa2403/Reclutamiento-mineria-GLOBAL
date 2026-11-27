package com.mineria.reclutamiento.modules.empresa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.modules.auth.entity.Rol;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import com.mineria.reclutamiento.modules.documento.entity.Documento;
import com.mineria.reclutamiento.modules.documento.entity.TipoDocumento;
import com.mineria.reclutamiento.modules.documento.repository.DocumentoRepository;
import com.mineria.reclutamiento.modules.documento.service.DocumentoService;
import com.mineria.reclutamiento.core.config.AwsProperties;
import com.mineria.reclutamiento.modules.empresa.dto.response.SolicitudEmpresaResponse;
import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa;
import com.mineria.reclutamiento.modules.empresa.entity.Representante;
import com.mineria.reclutamiento.modules.empresa.repository.EmpresaRepository;
import com.mineria.reclutamiento.modules.empresa.repository.RepresentanteRepository;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.persona.repository.PersonaRepository;
import com.mineria.reclutamiento.modules.empresa.entity.TipoActividadEmpresa;
import java.util.Optional;
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
class SolicitudEmpresaServiceTest {

    private static final String CORREO = "empresa@prueba.com";

    @Mock UsuarioRepository usuarioRepository;
    @Mock PersonaRepository personaRepository;
    @Mock RepresentanteRepository representanteRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock DocumentoRepository documentoRepository;
    @Mock DocumentoService documentoService;
    @Mock PlatformTransactionManager txManager;
    @Mock ActividadEmpresaService actividad;

    SolicitudEmpresaService service;
    Empresa empresa;
    Usuario usuario;
    MockMultipartFile pdf = new MockMultipartFile("archivo", "poder.pdf", "application/pdf", "%PDF-1.7".getBytes());
    DocumentoService.ArchivoValidado validado =
            new DocumentoService.ArchivoValidado("poder.pdf", "application/pdf", "pdf", "%PDF-1.7".getBytes(), "h".repeat(64));

    @BeforeEach
    void setUp() {
        service = new SolicitudEmpresaService(usuarioRepository, personaRepository, representanteRepository,
                empresaRepository, documentoRepository, documentoService,
                new AwsProperties("us-east-2", "bucket-prueba", "", "cvs", 300), txManager, actividad);

        usuario = new Usuario(CORREO, Rol.EMPRESA);
        Persona persona = new Persona(3L, usuario, "12345678", "ANA", "RUIZ", "987654321", null);
        empresa = new Empresa("20123456789", "MINERA REAL S.A.C.", null, null, CORREO, null);
        Representante representante = new Representante(1L, persona, empresa, "Gerente");

        when(txManager.getTransaction(any())).thenAnswer(inv -> new SimpleTransactionStatus());
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(personaRepository.findByUsuarioId(any())).thenReturn(Optional.of(persona));
        when(representanteRepository.findByPersona_IdPersona(3L)).thenReturn(Optional.of(representante));
        when(documentoService.validar(eq(pdf), any())).thenReturn(validado);
        when(documentoService.subirAlAlmacenamiento(any(), eq(TipoDocumento.ACREDITACION_EMPRESA), eq(validado)))
                .thenReturn("acreditacion_empresa/1/nuevo.pdf");
        when(documentoService.registrar(any(), any(), any(), any())).thenAnswer(inv -> documento(inv.getArgument(3)));
    }

    @Test
    void empresaNueva_empiezaEsperandoSuDocumento() {
        SolicitudEmpresaResponse res = service.obtenerMiSolicitud(CORREO);
        assertThat(res.estado()).isEqualTo(EstadoEmpresa.PENDIENTE_DOCUMENTO);
        assertThat(res.puedeSubirDocumento()).isTrue();
        assertThat(res.documentoAcreditacion()).isNull();
    }

    @Test
    void subirDocumento_dejaLaSolicitudEnRevision() {
        SolicitudEmpresaResponse res = service.subirAcreditacion(CORREO, pdf);

        assertThat(res.estado()).isEqualTo(EstadoEmpresa.EN_REVISION);
        assertThat(res.puedeSubirDocumento()).isFalse();
        verify(actividad).registrar(empresa, TipoActividadEmpresa.DOCUMENTO_ENVIADO, null, null);
        assertThat(empresa.getDocumentoAcreditacion().getClaveAlmacenamiento()).isEqualTo("acreditacion_empresa/1/nuevo.pdf");
        verify(documentoService, never()).borrarArchivo(any());
    }

    @Test
    void enRevision_noPuedeSubirOtroDocumento_niSeSubeNadaAlStorage() {
        empresa.setEstado(EstadoEmpresa.EN_REVISION);

        assertThatThrownBy(() -> service.subirAcreditacion(CORREO, pdf))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("en revisión");
        verify(documentoService, never()).subirAlAlmacenamiento(any(), any(), any());
    }

    @Test
    void observada_reemplazaElDocumento_yBorraElAnteriorDelStorage() {
        empresa.setEstado(EstadoEmpresa.OBSERVADA);
        empresa.setMotivoRevision("El documento está ilegible");
        Documento anterior = documento("acreditacion_empresa/1/anterior.pdf");
        empresa.setDocumentoAcreditacion(anterior);

        SolicitudEmpresaResponse res = service.subirAcreditacion(CORREO, pdf);

        assertThat(res.estado()).isEqualTo(EstadoEmpresa.EN_REVISION);
        verify(documentoRepository).delete(anterior);
        verify(documentoService).borrarArchivo("acreditacion_empresa/1/anterior.pdf");
    }

    @Test
    void siFallaLaBd_seBorraElArchivoRecienSubido_yNoCambiaElEstado() {
        when(documentoService.registrar(any(), any(), any(), any())).thenThrow(new IllegalStateException("BD caída"));

        assertThatThrownBy(() -> service.subirAcreditacion(CORREO, pdf)).isInstanceOf(IllegalStateException.class);

        verify(documentoService).borrarArchivo("acreditacion_empresa/1/nuevo.pdf");
        verify(txManager).rollback(any());
        assertThat(empresa.getEstado()).isEqualTo(EstadoEmpresa.PENDIENTE_DOCUMENTO);
    }

    @Test
    void archivoInvalido_noLlegaAlStorage() {
        when(documentoService.validar(eq(pdf), any())).thenThrow(new BadRequestException("Formato de archivo no permitido"));

        assertThatThrownBy(() -> service.subirAcreditacion(CORREO, pdf)).isInstanceOf(BadRequestException.class);
        verify(documentoService, never()).subirAlAlmacenamiento(anyLong(), any(), any());
    }

    private Documento documento(String clave) {
        Documento d = mock(Documento.class);
        when(d.getClaveAlmacenamiento()).thenReturn(clave);
        when(d.getNombreOriginal()).thenReturn("poder.pdf");
        when(d.getTipoMime()).thenReturn("application/pdf");
        return d;
    }
}
