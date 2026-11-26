package com.mineria.reclutamiento.modules.postulacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.auth.entity.Rol;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.convocatoria.entity.Convocatoria;
import com.mineria.reclutamiento.modules.convocatoria.repository.ConvocatoriaRepository;
import com.mineria.reclutamiento.modules.documento.service.DocumentoService;
import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import com.mineria.reclutamiento.modules.empresa.service.EmpresaActual;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.postulacion.entity.EstadoPostulacion;
import com.mineria.reclutamiento.modules.postulacion.entity.Postulacion;
import com.mineria.reclutamiento.modules.postulacion.repository.PostulacionRepository;
import com.mineria.reclutamiento.modules.postulante.entity.Certificacion;
import com.mineria.reclutamiento.modules.postulante.entity.DocumentoPostulante;
import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import com.mineria.reclutamiento.modules.postulante.dto.response.PerfilProfesionalResponse;
import com.mineria.reclutamiento.modules.postulante.service.PerfilProfesionalService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CandidatoServiceTest {

    static final String CORREO_EMPRESA = "empresa@prueba.pe";
    static final LocalDate HOY = LocalDate.of(2026, 10, 7);

    @Mock EmpresaActual empresaActual;
    @Mock ConvocatoriaRepository convocatoriaRepository;
    @Mock PostulacionRepository postulacionRepository;
    @Mock DocumentoService documentoService;
    @Mock PlatformTransactionManager txManager;
    @Mock PerfilProfesionalService perfilProfesionalService;

    CandidatoService service;
    Usuario reclutador;
    Postulacion conSctr;
    Postulacion sinCertificados;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        service = new CandidatoService(empresaActual, convocatoriaRepository, postulacionRepository,
                documentoService, reloj, txManager, perfilProfesionalService);
        when(perfilProfesionalService.obtenerDe(org.mockito.ArgumentMatchers.any())).thenReturn(new PerfilProfesionalResponse(
                "Operador con experiencia", List.of(), List.of(), List.of(), List.of(), List.of(), true, List.of()));
        when(txManager.getTransaction(org.mockito.ArgumentMatchers.any())).thenReturn(new SimpleTransactionStatus());

        Empresa propia = empresa(1L);
        reclutador = new Usuario(CORREO_EMPRESA, Rol.EMPRESA);
        when(empresaActual.obtenerAprobada(CORREO_EMPRESA)).thenReturn(new EmpresaActual.Contexto(reclutador, propia));

        Convocatoria convocatoria = mock(Convocatoria.class);
        when(convocatoria.getId()).thenReturn(5L);
        when(convocatoria.getEmpresa()).thenReturn(propia);
        when(convocatoriaRepository.findById(5L)).thenReturn(Optional.of(convocatoria));
        Convocatoria ajena = mock(Convocatoria.class);
        Empresa otra = empresa(2L);
        when(ajena.getEmpresa()).thenReturn(otra);
        when(convocatoriaRepository.findById(6L)).thenReturn(Optional.of(ajena));

        Certificacion sctr = mock(Certificacion.class);
        when(sctr.getId()).thenReturn(30L);
        when(sctr.getNombre()).thenReturn("SCTR Salud");
        conSctr = new Postulacion(convocatoria, postulante(3L, 7L), Set.of(sctr));
        ReflectionTestUtils.setField(conSctr, "id", 100L);
        sinCertificados = new Postulacion(convocatoria, postulante(4L, 8L), Set.of());
        ReflectionTestUtils.setField(sinCertificados, "id", 101L);
        when(postulacionRepository.deConvocatoria(5L))
                .thenReturn(List.of(conSctr, sinCertificados));
        when(postulacionRepository.findById(100L)).thenReturn(Optional.of(conSctr));
    }

    @Test
    void candidatos_enOrdenDeLlegada() {
        assertThat(service.candidatos(CORREO_EMPRESA, 5L)).extracting(c -> c.idPostulacion()).containsExactly(100L, 101L);
    }

    @Test
    void candidatos_incluyenElPerfilProfesional() {
        assertThat(service.candidatos(CORREO_EMPRESA, 5L))
                .allSatisfy(c -> assertThat(c.resumenProfesional()).isEqualTo("Operador con experiencia"));
    }

    @Test
    void convocatoriaDeOtraEmpresa_404() {
        assertThatThrownBy(() -> service.candidatos(CORREO_EMPRESA, 6L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cambiarEstado_registraCuando_yNoPermiteVolverAPostulado() {
        var res = service.cambiarEstado(CORREO_EMPRESA, 100L, EstadoPostulacion.ACEPTADO);
        assertThat(res.estado()).isEqualTo(EstadoPostulacion.ACEPTADO);
        assertThat(conSctr.getFechaCambioEstado()).isNotNull();

        assertThatThrownBy(() -> service.cambiarEstado(CORREO_EMPRESA, 100L, EstadoPostulacion.POSTULADO))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.cambiarEstado(CORREO_EMPRESA, 999L, EstadoPostulacion.ACEPTADO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void confirmado_soloPuedePasarADescartado() {
        service.cambiarEstado(CORREO_EMPRESA, 100L, EstadoPostulacion.ACEPTADO);
        conSctr.confirmar(conSctr.getFechaCambioEstado());

        assertThatThrownBy(() -> service.cambiarEstado(CORREO_EMPRESA, 100L, EstadoPostulacion.ACEPTADO))
                .isInstanceOf(BadRequestException.class);
        assertThat(service.cambiarEstado(CORREO_EMPRESA, 100L, EstadoPostulacion.DESCARTADO).estado())
                .isEqualTo(EstadoPostulacion.DESCARTADO);
    }

    @Test
    void documentoDelCandidato_soloSiExiste_yCertificadoSoloSiFueAdjuntado() {
        assertThatThrownBy(() -> service.urlDocumento(CORREO_EMPRESA, 100L, DocumentoPostulante.CV))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.urlCertificado(CORREO_EMPRESA, 100L, 31L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("no adjunta");
        verify(documentoService, never()).urlFirmada(anyString());
    }

    @Test
    void aniosExperiencia_noCuentaDosVecesLosPeriodosSolapados() {
        var a = experiencia(LocalDate.of(2018, 1, 1), LocalDate.of(2021, 1, 1));
        var b = experiencia(LocalDate.of(2020, 1, 1), LocalDate.of(2022, 1, 1));
        var c = experiencia(LocalDate.of(2024, 1, 1), null);
        assertThat(CandidatoService.aniosExperiencia(List.of(a, b, c), HOY)).isEqualTo(6);
        assertThat(CandidatoService.aniosExperiencia(List.of(), HOY)).isZero();
    }


    private static Empresa empresa(Long id) {
        Empresa e = mock(Empresa.class);
        when(e.getId()).thenReturn(id);
        return e;
    }

    private static Postulante postulante(Long idPostulante, Long idUsuario) {
        Usuario u = new Usuario("p" + idUsuario + "@prueba.pe", Rol.POSTULANTE);
        ReflectionTestUtils.setField(u, "id", idUsuario);
        Postulante p = new Postulante();
        ReflectionTestUtils.setField(p, "idPostulante", idPostulante);
        p.setPersona(new Persona(idPostulante, u, "1234567" + idPostulante, "NOMBRE", "APELLIDO", "90000000" + idPostulante, null));
        return p;
    }

    private static PerfilProfesionalResponse.Experiencia experiencia(LocalDate inicio, LocalDate fin) {
        return new PerfilProfesionalResponse.Experiencia("Operador", "Minera", inicio, fin, null);
    }
}
