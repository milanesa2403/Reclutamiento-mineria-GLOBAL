package com.mineria.reclutamiento.modules.postulacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.core.exception.DuplicateResourceException;
import com.mineria.reclutamiento.modules.auth.entity.Rol;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.convocatoria.entity.Convocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.EstadoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.TipoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.repository.ConvocatoriaRepository;
import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.postulacion.dto.request.PostularRequest;
import com.mineria.reclutamiento.modules.postulacion.entity.Postulacion;
import com.mineria.reclutamiento.modules.postulacion.repository.PostulacionRepository;
import com.mineria.reclutamiento.modules.postulante.entity.CatalogoCertificacion;
import com.mineria.reclutamiento.modules.postulante.entity.Certificacion;
import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import com.mineria.reclutamiento.modules.postulante.repository.CertificacionRepository;
import com.mineria.reclutamiento.modules.postulante.service.EstadoPerfilProfesional;
import com.mineria.reclutamiento.modules.postulante.service.PostulanteService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostulacionServiceTest {

    static final String CORREO = "post@prueba.pe";
    static final LocalDate HOY = LocalDate.of(2026, 10, 7);

    @Mock PostulanteService postulanteService;
    @Mock ConvocatoriaRepository convocatoriaRepository;
    @Mock PostulacionRepository postulacionRepository;
    @Mock CertificacionRepository certificacionRepository;
    @Mock EstadoPerfilProfesional estadoPerfil;

    PostulacionService service;
    Postulante postulante;
    Convocatoria convocatoria;
    Empresa empresa;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        service = new PostulacionService(postulanteService, convocatoriaRepository, postulacionRepository,
                certificacionRepository, estadoPerfil, reloj);

        Usuario usuario = new Usuario(CORREO, Rol.POSTULANTE);
        ReflectionTestUtils.setField(usuario, "id", 7L);
        postulante = new Postulante();
        ReflectionTestUtils.setField(postulante, "idPostulante", 3L);
        postulante.setPersona(new Persona(10L, usuario, "12345678", "ANA", "RUIZ", "987654321", null));
        postulante.setUrlCv("cvs/cv/7/11111111-1111-1111-1111-111111111111.pdf");

        empresa = mock(Empresa.class);
        when(empresa.estaAprobada()).thenReturn(true);
        when(empresa.getEstado()).thenReturn(com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa.APROBADA);
        when(empresa.getRazonSocial()).thenReturn("MINERA S.A.C.");
        convocatoria = mock(Convocatoria.class);
        when(convocatoria.getId()).thenReturn(5L);
        when(convocatoria.getEmpresa()).thenReturn(empresa);
        when(convocatoria.getEstado()).thenReturn(EstadoConvocatoria.PUBLICADA);
        when(convocatoria.getTipo()).thenReturn(TipoConvocatoria.PARADA_PLANTA);

        when(postulanteService.postulanteDe(CORREO)).thenReturn(postulante);
        when(convocatoriaRepository.findById(5L)).thenReturn(Optional.of(convocatoria));
        when(estadoPerfil.pendientes(3L)).thenReturn(List.of());
        when(postulacionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void postular_guardaLaPostulacionConSusCertificaciones() {
        Certificacion sctr = mock(Certificacion.class);
        when(sctr.getNombre()).thenReturn("SCTR");
        when(certificacionRepository.findByIdInAndPostulante_IdPostulante(any(), any())).thenReturn(List.of(sctr));

        var res = service.postular(CORREO, new PostularRequest(5L, List.of(9L, 9L), null));

        ArgumentCaptor<Postulacion> guardada = ArgumentCaptor.forClass(Postulacion.class);
        verify(postulacionRepository).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getPostulante()).isSameAs(postulante);
        assertThat(guardada.getValue().getCertificaciones()).containsExactly(sctr);
        assertThat(res.empresa()).isEqualTo("MINERA S.A.C.");
        assertThat(res.certificacionesAdjuntas()).containsExactly("SCTR");
    }

    @Test
    void convocatoriaNoPublicada_cerrada_oDeEmpresaNoAprobada_noAdmitePostulaciones() {
        when(convocatoria.getEstado()).thenReturn(EstadoConvocatoria.BORRADOR);
        assertThatThrownBy(() -> service.postular(CORREO, new PostularRequest(5L, null, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("no está disponible");

        when(convocatoria.getEstado()).thenReturn(EstadoConvocatoria.PUBLICADA);
        when(convocatoria.getFechaCierre()).thenReturn(HOY.minusDays(1));
        assertThatThrownBy(() -> service.postular(CORREO, new PostularRequest(5L, null, null)))
                .isInstanceOf(BadRequestException.class);

        when(convocatoria.getFechaCierre()).thenReturn(HOY);
        when(empresa.estaAprobada()).thenReturn(false);
        when(empresa.getEstado()).thenReturn(com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa.EN_REVISION);
        assertThatThrownBy(() -> service.postular(CORREO, new PostularRequest(5L, null, null)))
                .isInstanceOf(BadRequestException.class);
        verify(postulacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void sinCvOPerfilIncompleto_noPuedePostular() {
        postulante.setUrlCv(null);
        assertThatThrownBy(() -> service.postular(CORREO, new PostularRequest(5L, null, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("CV");

        postulante.setUrlCv("cvs/cv/7/11111111-1111-1111-1111-111111111111.pdf");
        when(estadoPerfil.pendientes(3L)).thenReturn(List.of("habilidades"));
        assertThatThrownBy(() -> service.postular(CORREO, new PostularRequest(5L, null, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("perfil");
        verify(postulacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void yaPostulo_409() {
        when(postulacionRepository.existe(5L, 3L)).thenReturn(true);
        assertThatThrownBy(() -> service.postular(CORREO, new PostularRequest(5L, null, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void certificacionesAjenas_400() {
        when(certificacionRepository.findByIdInAndPostulante_IdPostulante(any(), any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.postular(CORREO, new PostularRequest(5L, List.of(99L), null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("propio perfil");
        verify(postulacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void requisitos_avisaCvYCertificaciones() {
        postulante.setUrlCv("https://x/public/cv.pdf");
        when(certificacionRepository.delPostulante(anyLong())).thenReturn(List.of());

        var r = service.requisitos(CORREO, null);

        assertThat(r.puedePostular()).isFalse();
        assertThat(r.tieneCv()).isFalse();
        assertThat(r.tieneCertificaciones()).isFalse();
        assertThat(r.faltantes()).hasSize(2);
    }


    static final CatalogoCertificacion SCTR = tipo(10L, "SCTR");
    static final CatalogoCertificacion MANEJO = tipo(11L, "Manejo defensivo");

    private static CatalogoCertificacion tipo(long id, String nombre) {
        CatalogoCertificacion t = new CatalogoCertificacion(nombre);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    private com.mineria.reclutamiento.modules.convocatoria.entity.RequisitoCertificado requisito(long id, CatalogoCertificacion tipo, boolean pdf) {
        var r = new com.mineria.reclutamiento.modules.convocatoria.entity.RequisitoCertificado(convocatoria, tipo, pdf);
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }

    private Certificacion certificacion(long id, String nombre, CatalogoCertificacion tipo, boolean conPdf) {
        Certificacion c = mock(Certificacion.class);
        when(c.getCatalogo()).thenReturn(tipo);
        when(c.getId()).thenReturn(id);
        when(c.getNombre()).thenReturn(nombre);
        when(c.getDocumento()).thenReturn(conPdf ? mock(com.mineria.reclutamiento.modules.documento.entity.Documento.class) : null);
        when(certificacionRepository.findByIdAndPostulante_IdPostulante(id, 3L)).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void requisitoExigido_debeCubrirse_yConPdfSiSeExige() {
        var sctr = requisito(70L, SCTR, true);
        when(convocatoria.getRequisitosCertificado()).thenReturn(List.of(sctr));
        certificacion(1L, "SCTR Salud", SCTR, false);

        assertThatThrownBy(() -> service.postular(CORREO, new PostularRequest(5L, null, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("SCTR");
        var cubre = List.of(new PostularRequest.RequisitoCubierto(70L, 1L));
        assertThatThrownBy(() -> service.postular(CORREO, new PostularRequest(5L, null, cubre)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("pide el PDF");
        var ajena = List.of(new PostularRequest.RequisitoCubierto(70L, 99L));
        assertThatThrownBy(() -> service.postular(CORREO, new PostularRequest(5L, null, ajena)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("propio perfil");
        var extra = List.of(new PostularRequest.RequisitoCubierto(70L, 2L), new PostularRequest.RequisitoCubierto(71L, 2L));
        certificacion(2L, "SCTR", SCTR, true);
        assertThatThrownBy(() -> service.postular(CORREO, new PostularRequest(5L, null, extra)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("no pertenece");
        verify(postulacionRepository, never()).saveAndFlush(any());
    }

    @Test
    void requisitoCubiertoConPdf_quedaRegistradoYAdjunto() {
        var sctr = requisito(70L, SCTR, true);
        when(convocatoria.getRequisitosCertificado()).thenReturn(List.of(sctr));
        Certificacion conPdf = certificacion(2L, "SCTR", SCTR, true);

        service.postular(CORREO, new PostularRequest(5L, null, List.of(new PostularRequest.RequisitoCubierto(70L, 2L))));

        ArgumentCaptor<Postulacion> guardada = ArgumentCaptor.forClass(Postulacion.class);
        verify(postulacionRepository).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getRequisitosCubiertos()).singleElement()
                .satisfies(r -> assertThat(r.getCertificacion()).isSameAs(conPdf));
        assertThat(guardada.getValue().getCertificaciones()).contains(conPdf);
    }

    @Test
    void requisitosDeUnaConvocatoria_sugierenCertificacionesYAvisanFaltaDePdf() {
        var sctr = requisito(70L, SCTR, true);
        when(convocatoria.getRequisitosCertificado()).thenReturn(List.of(sctr));
        Certificacion sinPdf = certificacion(1L, "Seguro SCTR Pensión", SCTR, false);
        Certificacion otra = certificacion(2L, "Manejo defensivo", MANEJO, true);
        when(certificacionRepository.delPostulante(3L)).thenReturn(List.of(otra, sinPdf));

        var r = service.requisitos(CORREO, 5L);

        var req = r.certificadosRequeridos().get(0);
        assertThat(req.opciones().get(0).idCertificacion()).isEqualTo(1L);
        assertThat(req.opciones().get(0).coincide()).isTrue();
        assertThat(req.cumplible()).isTrue();
        assertThat(r.faltantes()).anyMatch(f -> f.contains("pide el PDF de \"SCTR\""));
        assertThat(PostulacionService.coincide(sctr, otra)).isFalse();
        assertThat(PostulacionService.coincide(sctr, certificacion(3L, "SCTR", null, true))).isFalse();
    }

    @Test
    void cruzan_soloParadasConMargenDeTresDias() {
        Convocatoria confirmada = labores("2026-12-05", "2026-12-20");
        Convocatoria planilla = mock(Convocatoria.class);
        when(planilla.getTipo()).thenReturn(TipoConvocatoria.PLANILLA_COMPLETA);
        assertThat(PostulacionService.cruzan(confirmada, labores("2026-12-10", "2026-12-25"))).isTrue();
        assertThat(PostulacionService.cruzan(confirmada, labores("2026-12-22", "2026-12-30"))).isTrue();
        assertThat(PostulacionService.cruzan(confirmada, labores("2026-12-25", "2027-01-05"))).isFalse();
        assertThat(PostulacionService.cruzan(confirmada, labores("2026-11-01", "2026-12-01"))).isFalse();
        assertThat(PostulacionService.cruzan(confirmada, planilla)).isFalse();
        assertThat(PostulacionService.cruzan(planilla, confirmada)).isFalse();
    }

    private static Convocatoria labores(String inicio, String fin) {
        Convocatoria c = mock(Convocatoria.class);
        when(c.getTipo()).thenReturn(TipoConvocatoria.PARADA_PLANTA);
        when(c.getFechaInicioLabores()).thenReturn(LocalDate.parse(inicio));
        when(c.getFechaFinLabores()).thenReturn(LocalDate.parse(fin));
        return c;
    }
}
