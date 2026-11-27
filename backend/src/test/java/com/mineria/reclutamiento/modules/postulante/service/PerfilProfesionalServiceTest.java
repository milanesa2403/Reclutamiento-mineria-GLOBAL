package com.mineria.reclutamiento.modules.postulante.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mineria.reclutamiento.core.exception.CamposInvalidosException;
import com.mineria.reclutamiento.modules.postulante.dto.request.PerfilProfesionalRequest;
import com.mineria.reclutamiento.modules.postulante.dto.request.PerfilProfesionalRequest.EspecialidadItem;
import com.mineria.reclutamiento.modules.postulante.dto.request.PerfilProfesionalRequest.ExperienciaItem;
import com.mineria.reclutamiento.modules.postulante.entity.EspecialidadPostulante;
import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import com.mineria.reclutamiento.modules.postulante.repository.CertificacionRepository;
import com.mineria.reclutamiento.modules.postulante.repository.EspecialidadPostulanteRepository;
import com.mineria.reclutamiento.modules.postulante.repository.ExperienciaLaboralRepository;
import com.mineria.reclutamiento.modules.postulante.repository.FormacionAcademicaRepository;
import com.mineria.reclutamiento.modules.postulante.repository.HabilidadPostulanteRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PerfilProfesionalServiceTest {

    private static final String CORREO_A = "a@prueba.com";

    @Mock PostulanteService postulanteService;
    @Mock EspecialidadPostulanteRepository especialidadRepository;
    @Mock HabilidadPostulanteRepository habilidadRepository;
    @Mock ExperienciaLaboralRepository experienciaRepository;
    @Mock FormacionAcademicaRepository formacionRepository;
    @Mock CertificacionRepository certificacionRepository;
    @Mock com.mineria.reclutamiento.modules.postulante.repository.CatalogoCertificacionRepository catalogoRepository;
    @Mock EstadoPerfilProfesional estadoPerfil;
    @Mock com.mineria.reclutamiento.modules.documento.repository.DocumentoRepository documentoRepository;
    @Mock com.mineria.reclutamiento.modules.documento.service.DocumentoService documentoService;

    PerfilProfesionalService service;
    Postulante postulanteA;

    @BeforeEach
    void setUp() {
        service = new PerfilProfesionalService(postulanteService, especialidadRepository, habilidadRepository,
                experienciaRepository, formacionRepository, certificacionRepository, catalogoRepository, estadoPerfil,
                documentoRepository, documentoService);
        postulanteA = mock(Postulante.class);
        when(postulanteA.getIdPostulante()).thenReturn(7L);
        when(postulanteService.postulanteDe(CORREO_A)).thenReturn(postulanteA);
        when(estadoPerfil.pendientes(anyLong())).thenReturn(List.of());
    }

    @Test
    void reemplazo_borraLoAnteriorDelPropioPostulante_yLuegoInserta() {
        var res = service.reemplazar(CORREO_A, valido());

        InOrder orden = Mockito.inOrder(especialidadRepository);
        orden.verify(especialidadRepository).borrarDelPostulante(7L);
        orden.verify(especialidadRepository).saveAll(any());
        verify(habilidadRepository).borrarDelPostulante(7L);
        verify(experienciaRepository).borrarDelPostulante(7L);
        verify(formacionRepository).borrarDelPostulante(7L);
        verify(certificacionRepository, never()).borrarDelPostulante(anyLong());
        verify(certificacionRepository, org.mockito.Mockito.atLeastOnce()).delPostulante(7L);
        verify(postulanteService).postulanteDe(CORREO_A);
        assertThat(res.perfilCompleto()).isTrue();
    }

    @Test
    void unaSolaEspecialidadSinMarcar_quedaComoPrincipal() {
        PerfilProfesionalRequest req = perfil(List.of(new EspecialidadItem("  Geología de minas  ", false)), null, null, null);

        service.reemplazar(CORREO_A, req);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EspecialidadPostulante>> guardadas = ArgumentCaptor.forClass(List.class);
        verify(especialidadRepository).saveAll(guardadas.capture());
        assertThat(guardadas.getValue()).singleElement().satisfies(e -> {
            assertThat(e.getNombre()).isEqualTo("Geología de minas");
            assertThat(e.isEsPrincipal()).isTrue();
        });
    }

    @Test
    void variasEspecialidades_exigenExactamenteUnaPrincipal() {
        PerfilProfesionalRequest ninguna =
                perfil(List.of(new EspecialidadItem("A", false), new EspecialidadItem("B", false)), null, null, null);
        assertThatThrownBy(() -> service.reemplazar(CORREO_A, ninguna))
                .isInstanceOf(CamposInvalidosException.class)
                .satisfies(e -> assertThat(((CamposInvalidosException) e).getErrores()).containsKey("especialidades"));

        PerfilProfesionalRequest dos =
                perfil(List.of(new EspecialidadItem("A", true), new EspecialidadItem("B", true)), null, null, null);
        assertThatThrownBy(() -> service.reemplazar(CORREO_A, dos)).isInstanceOf(CamposInvalidosException.class);

        verify(especialidadRepository, never()).borrarDelPostulante(anyLong());
    }

    @Test
    void duplicados_sinDistinguirMayusculas_seMarcanEnSuPosicion() {
        PerfilProfesionalRequest req = perfil(
                List.of(new EspecialidadItem("SSOMA", true), new EspecialidadItem("ssoma", false)),
                List.of("Scoop", "scoop "), null, null);

        assertThatThrownBy(() -> service.reemplazar(CORREO_A, req))
                .isInstanceOf(CamposInvalidosException.class)
                .satisfies(e -> assertThat(((CamposInvalidosException) e).getErrores())
                        .containsKeys("habilidades[1]", "especialidades[1].nombre"));
    }

    @Test
    void fechaFinAnteriorAlInicio_esError_yFinVacioEsTrabajoActual() {
        PerfilProfesionalRequest req = perfil(null, null, List.of(
                new ExperienciaItem("Operador", "Minera Sur", LocalDate.of(2020, 1, 1), null, null),
                new ExperienciaItem("Ayudante", "Minera Norte", LocalDate.of(2019, 5, 1), LocalDate.of(2018, 1, 1), null)),
                null);

        assertThatThrownBy(() -> service.reemplazar(CORREO_A, req))
                .isInstanceOf(CamposInvalidosException.class)
                .satisfies(e -> assertThat(((CamposInvalidosException) e).getErrores())
                        .containsOnlyKeys("experiencias[1].fechaFin"));
    }

    @Test
    void perfilIncompleto_seInformaConSusPendientes() {
        when(estadoPerfil.pendientes(7L)).thenReturn(List.of("especialidad", "habilidades"));
        var res = service.obtener(CORREO_A);
        assertThat(res.perfilCompleto()).isFalse();
        assertThat(res.pendientes()).containsExactly("especialidad", "habilidades");
        verify(especialidadRepository, never()).saveAll(any());
    }

    private static PerfilProfesionalRequest valido() {
        return perfil(null, null, null, null);
    }

    private static PerfilProfesionalRequest perfil(List<EspecialidadItem> especialidades, List<String> habilidades,
            List<ExperienciaItem> experiencias, List<PerfilProfesionalRequest.CertificacionItem> certificaciones) {
        return new PerfilProfesionalRequest(null,
                especialidades != null ? especialidades
                        : List.of(new EspecialidadItem("Operación de maquinaria pesada", true)),
                habilidades != null ? habilidades : List.of("Scoop", "Jumbo"),
                experiencias != null ? experiencias
                        : List.of(new ExperienciaItem("Operador", "Minera Sur", LocalDate.of(2020, 1, 1), null, null)),
                null, certificaciones);
    }

    @Test
    void certificaciones_conservanSuPdf_yLasQuitadasBorranElSuyoDespuesDelCommit() {
        var conPdf = mock(com.mineria.reclutamiento.modules.postulante.entity.Certificacion.class);
        var quitada = mock(com.mineria.reclutamiento.modules.postulante.entity.Certificacion.class);
        var pdfQuitada = mock(com.mineria.reclutamiento.modules.documento.entity.Documento.class);
        when(conPdf.getId()).thenReturn(1L);
        when(quitada.getId()).thenReturn(2L);
        when(quitada.getDocumento()).thenReturn(pdfQuitada);
        when(pdfQuitada.getClaveAlmacenamiento()).thenReturn("certificado/9/viejo.pdf");
        when(certificacionRepository.delPostulante(7L)).thenReturn(List.of(conPdf, quitada));

        var editada = new PerfilProfesionalRequest.CertificacionItem(1L, null, " SCTR ", " Rimac ", null);
        var ajena = new PerfilProfesionalRequest.CertificacionItem(555L, null, "Manejo 4x4", "Mina", null);
        PerfilProfesionalRequest req = perfil(null, null, null, List.of(editada, ajena));

        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            service.reemplazar(CORREO_A, req);
            verify(conPdf).actualizar(null, "SCTR", "Rimac", null);
            verify(certificacionRepository).deleteAll(List.of(quitada));
            verify(documentoRepository).deleteAll(List.of(pdfQuitada));
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<com.mineria.reclutamiento.modules.postulante.entity.Certificacion>> nuevas =
                    ArgumentCaptor.forClass(List.class);
            verify(certificacionRepository).saveAll(nuevas.capture());
            assertThat(nuevas.getValue()).singleElement().satisfies(c -> assertThat(c.getNombre()).isEqualTo("Manejo 4x4"));
            verify(documentoService, never()).borrarArchivo(org.mockito.ArgumentMatchers.anyString());
            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()
                    .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);
            verify(documentoService).borrarArchivo("certificado/9/viejo.pdf");
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void certificacionConTipoDelCatalogo_tomaElNombreDelCatalogo_yUnTipoInexistenteEsErrorDeCampo() {
        var sctr = new com.mineria.reclutamiento.modules.postulante.entity.CatalogoCertificacion("SCTR");
        org.springframework.test.util.ReflectionTestUtils.setField(sctr, "id", 1L);
        when(catalogoRepository.findAllById(List.of(1L))).thenReturn(List.of(sctr));

        var conTipo = new PerfilProfesionalRequest.CertificacionItem(null, 1L, "lo que sea", "Rimac", null);
        PerfilProfesionalRequest req = perfil(null, null, null, List.of(conTipo));
        service.reemplazar(CORREO_A, req);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.mineria.reclutamiento.modules.postulante.entity.Certificacion>> nuevas =
                ArgumentCaptor.forClass(List.class);
        verify(certificacionRepository).saveAll(nuevas.capture());
        assertThat(nuevas.getValue()).singleElement().satisfies(c -> {
            assertThat(c.getNombre()).isEqualTo("SCTR");
            assertThat(c.getCatalogo()).isSameAs(sctr);
        });

        var tipoInexistente = perfil(null, null, null,
                List.of(new PerfilProfesionalRequest.CertificacionItem(null, 99L, "lo que sea", "Rimac", null)));
        assertThatThrownBy(() -> service.reemplazar(CORREO_A, tipoInexistente)).isInstanceOf(CamposInvalidosException.class)
                .satisfies(e -> assertThat(((CamposInvalidosException) e).getErrores())
                        .containsKey("certificaciones[0].idCatalogoCertificacion"));
    }

    @Test
    void noSeQuitaUnaCertificacionQueRespaldaUnaPostulacionEnCurso() {
        var usada = mock(com.mineria.reclutamiento.modules.postulante.entity.Certificacion.class);
        when(usada.getId()).thenReturn(4L);
        when(certificacionRepository.delPostulante(7L)).thenReturn(List.of(usada));
        when(certificacionRepository.puestosQueLasUsan(List.of(4L), List.of(
                com.mineria.reclutamiento.modules.postulacion.entity.EstadoPostulacion.POSTULADO,
                com.mineria.reclutamiento.modules.postulacion.entity.EstadoPostulacion.EN_REVISION)))
                .thenReturn(List.of("Soldador"));

        PerfilProfesionalRequest req = perfil(null, null, null, List.of());
        assertThatThrownBy(() -> service.reemplazar(CORREO_A, req))
                .isInstanceOf(com.mineria.reclutamiento.core.exception.BadRequestException.class)
                .hasMessageContaining("Soldador");
        verify(certificacionRepository, never()).deleteAll(any());
    }
}
