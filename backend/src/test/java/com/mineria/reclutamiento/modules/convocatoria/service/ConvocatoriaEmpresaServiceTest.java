package com.mineria.reclutamiento.modules.convocatoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.core.exception.CamposInvalidosException;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.auth.entity.Rol;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.convocatoria.dto.ConvocatoriaRequest;
import com.mineria.reclutamiento.modules.convocatoria.dto.ConvocatoriaRequest.RequisitoCertificadoRequest;
import com.mineria.reclutamiento.modules.convocatoria.entity.Busqueda;
import com.mineria.reclutamiento.modules.convocatoria.entity.Convocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.EstadoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.TipoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.repository.BusquedaRepository;
import com.mineria.reclutamiento.modules.convocatoria.repository.ConvocatoriaRepository;
import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import com.mineria.reclutamiento.modules.empresa.service.EmpresaActual;
import com.mineria.reclutamiento.modules.postulacion.repository.PostulacionRepository;
import com.mineria.reclutamiento.modules.postulante.entity.CatalogoCertificacion;
import com.mineria.reclutamiento.modules.postulante.repository.CatalogoCertificacionRepository;
import com.mineria.reclutamiento.modules.ubigeo.entity.Ubigeo;
import com.mineria.reclutamiento.modules.ubigeo.repository.UbigeoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConvocatoriaEmpresaServiceTest {

    static final String CORREO = "empresa@prueba.pe";
    static final LocalDate HOY = LocalDate.of(2026, 10, 9);

    @Mock EmpresaActual empresaActual;
    @Mock ConvocatoriaRepository convocatoriaRepository;
    @Mock UbigeoRepository ubigeoRepository;
    @Mock PostulacionRepository postulacionRepository;
    @Mock BusquedaRepository busquedaRepository;
    @Mock CatalogoCertificacionRepository catalogoRepository;

    ConvocatoriaEmpresaService service;
    Empresa empresa;
    Usuario usuario;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        service = new ConvocatoriaEmpresaService(empresaActual, convocatoriaRepository, ubigeoRepository,
                postulacionRepository, busquedaRepository, catalogoRepository, reloj);
        empresa = mock(Empresa.class);
        when(empresa.getId()).thenReturn(1L);
        when(empresa.getRazonSocial()).thenReturn("MINERA S.A.C.");
        usuario = new Usuario(CORREO, Rol.EMPRESA);
        when(empresaActual.obtenerAprobada(CORREO)).thenReturn(new EmpresaActual.Contexto(usuario, empresa));
        Ubigeo huancayo = mock(Ubigeo.class);
        when(huancayo.getCodigo()).thenReturn("120101");
        when(ubigeoRepository.findById("120101")).thenReturn(Optional.of(huancayo));
        when(convocatoriaRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        List<CatalogoCertificacion> catalogo = List.of(tipo(1L, "SCTR"), tipo(2L, "Trabajos en altura"));
        when(catalogoRepository.findAllById(any())).thenAnswer(inv -> catalogo.stream()
                .filter(t -> ((Collection<?>) inv.getArgument(0)).contains(t.getId())).toList());
    }

    private static CatalogoCertificacion tipo(Long id, String nombre) {
        CatalogoCertificacion t = new CatalogoCertificacion(nombre);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    private static ConvocatoriaRequest request(TipoConvocatoria tipo, LocalDate inicio, LocalDate fin, String direccion,
                                               List<RequisitoCertificadoRequest> certs) {
        return new ConvocatoriaRequest(tipo, " Soldador ", "Cerro Verde", "12x12", "Parada de planta", "SCTR vigente",
                null, 2, inicio, fin, null, "120101", direccion, null, certs, null, null);
    }

    @Test
    void crear_quedaEnBorrador_conUbicacionYCertificadosExigidos() {
        var res = service.crear(CORREO, request(TipoConvocatoria.PARADA_PLANTA, HOY.plusDays(10), HOY.plusDays(20),
                "Km 45 Carretera Central", List.of(new RequisitoCertificadoRequest(1L, true),
                        new RequisitoCertificadoRequest(2L, false))));

        assertThat(res.estado()).isEqualTo(EstadoConvocatoria.BORRADOR);
        assertThat(res.puesto()).isEqualTo("Soldador");
        assertThat(res.ubicacion().codigo()).isEqualTo("120101");
        assertThat(res.certificadosRequeridos()).extracting(c -> c.nombre() + ":" + c.requierePdf())
                .containsExactly("SCTR:true", "Trabajos en altura:false");
        assertThat(res.vacantes()).isEqualTo(1);
        assertThat(res.busqueda()).isNull();
    }

    @Test
    void paradaDePlanta_exigeFechasDeLaboresYDireccion() {
        assertThatThrownBy(() -> service.crear(CORREO, request(TipoConvocatoria.PARADA_PLANTA, null, null, " ", null)))
                .isInstanceOf(CamposInvalidosException.class)
                .satisfies(e -> assertThat(((CamposInvalidosException) e).getErrores())
                        .containsKeys("fechaInicioLabores", "fechaFinLabores", "direccion"));
    }

    @Test
    void planillaCompleta_noExigeDireccion_yDescartaLasFechasDeLabores() {
        var res = service.crear(CORREO, request(TipoConvocatoria.PLANILLA_COMPLETA, HOY, HOY.plusDays(5), null, null));
        assertThat(res.fechaInicioLabores()).isNull();
        assertThat(res.fechaFinLabores()).isNull();
    }

    @Test
    void distritoInexistente_certificadoRepetidoOInexistente_sonErroresDeCampo() {
        var r = new ConvocatoriaRequest(TipoConvocatoria.PLANILLA_COMPLETA, "Operador", "Antamina", "14x7", "d", "r",
                null, null, null, null, HOY.minusDays(1), "999999", null, null,
                List.of(new RequisitoCertificadoRequest(1L, false), new RequisitoCertificadoRequest(1L, true),
                        new RequisitoCertificadoRequest(99L, false)),
                null, null);
        assertThatThrownBy(() -> service.crear(CORREO, r)).isInstanceOf(CamposInvalidosException.class)
                .satisfies(e -> assertThat(((CamposInvalidosException) e).getErrores())
                        .containsKeys("codigoUbigeo", "fechaCierre", "certificadosRequeridos[1].idCatalogoCertificacion",
                                "certificadosRequeridos[2].idCatalogoCertificacion"));
        verify(convocatoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void cicloDeVida_borradorPublicadaCerrada() {
        Convocatoria c = new Convocatoria(empresa, usuario, TipoConvocatoria.PLANILLA_COMPLETA);
        when(convocatoriaRepository.findById(5L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.cerrar(CORREO, 5L)).isInstanceOf(BadRequestException.class);
        assertThat(service.publicar(CORREO, 5L).estado()).isEqualTo(EstadoConvocatoria.PUBLICADA);
        assertThat(c.getPublicador()).isSameAs(usuario);
        assertThatThrownBy(() -> service.actualizar(CORREO, 5L, request(TipoConvocatoria.PLANILLA_COMPLETA, null, null, null, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("borrador");
        when(postulacionRepository.countByConvocatoria_Id(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.eliminar(CORREO, 5L)).isInstanceOf(BadRequestException.class);
        verify(convocatoriaRepository, never()).delete(any(Convocatoria.class));
        assertThat(service.cerrar(CORREO, 5L).estado()).isEqualTo(EstadoConvocatoria.CERRADA);
    }

    @Test
    void cambiarFechaCierre_ampliaLaPublicadaYReabreLaCerrada() {
        Convocatoria c = new Convocatoria(empresa, usuario, TipoConvocatoria.PLANILLA_COMPLETA);
        when(convocatoriaRepository.findById(5L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.cambiarFechaCierre(CORREO, 5L, HOY.plusDays(5)))
                .isInstanceOf(BadRequestException.class);

        service.publicar(CORREO, 5L);
        var publicada = service.cambiarFechaCierre(CORREO, 5L, HOY.plusDays(10));
        assertThat(publicada.fechaCierre()).isEqualTo(HOY.plusDays(10));
        assertThat(publicada.estado()).isEqualTo(EstadoConvocatoria.PUBLICADA);

        service.cerrar(CORREO, 5L);
        assertThatThrownBy(() -> service.cambiarFechaCierre(CORREO, 5L, HOY.minusDays(1)))
                .isInstanceOf(CamposInvalidosException.class);
        assertThat(c.getEstado()).isEqualTo(EstadoConvocatoria.CERRADA);

        var reabierta = service.cambiarFechaCierre(CORREO, 5L, HOY);
        assertThat(reabierta.estado()).isEqualTo(EstadoConvocatoria.PUBLICADA);
        assertThat(reabierta.fechaCierre()).isEqualTo(HOY);
    }

    @Test
    void listar_cierraAntesLasVencidasDeLaEmpresa() {
        Convocatoria vencida = new Convocatoria(empresa, usuario, TipoConvocatoria.PLANILLA_COMPLETA);
        vencida.publicar(usuario, HOY.minusDays(10).atStartOfDay());
        vencida.setFechaCierre(HOY.minusDays(1));
        when(convocatoriaRepository.vencidasDeEmpresa(1L, EstadoConvocatoria.PUBLICADA, HOY))
                .thenReturn(List.of(vencida));
        when(convocatoriaRepository.deEmpresa(1L)).thenReturn(List.of(vencida));

        assertThat(service.listar(CORREO, null)).singleElement()
                .satisfies(c -> assertThat(c.estado()).isEqualTo(EstadoConvocatoria.CERRADA));
    }

    @Test
    void publicadaSinPostulantes_sePuedeEliminar() {
        Convocatoria c = new Convocatoria(empresa, usuario, TipoConvocatoria.PLANILLA_COMPLETA);
        when(convocatoriaRepository.findById(5L)).thenReturn(Optional.of(c));
        service.publicar(CORREO, 5L);
        when(postulacionRepository.countByConvocatoria_Id(any())).thenReturn(0L);

        service.eliminar(CORREO, 5L);

        verify(convocatoriaRepository).delete(c);
    }

    @Test
    void noSePublicaConFechaDeCierreVencida() {
        Convocatoria c = new Convocatoria(empresa, usuario, TipoConvocatoria.PLANILLA_COMPLETA);
        c.setFechaCierre(HOY.minusDays(1));
        when(convocatoriaRepository.findById(5L)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.publicar(CORREO, 5L)).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("fecha de cierre");
    }

    private ConvocatoriaRequest planilla(Integer vacantes, Long idBusqueda) {
        return new ConvocatoriaRequest(TipoConvocatoria.PLANILLA_COMPLETA, "Operador", "Antamina", "14x7", "d", "r",
                null, 2, null, null, null, "120101", null, null, null, vacantes, idBusqueda);
    }

    @Test
    void enUnaBusquedaPropia_conVacantes_yMoverlaASuelta() {
        Busqueda parada = new Busqueda(empresa, "Parada octubre 2026");
        ReflectionTestUtils.setField(parada, "id", 7L);
        when(busquedaRepository.findById(7L)).thenReturn(Optional.of(parada));

        var res = service.crear(CORREO, planilla(10, 7L));
        assertThat(res.vacantes()).isEqualTo(10);
        assertThat(res.busqueda().nombre()).isEqualTo("Parada octubre 2026");

        Convocatoria c = new Convocatoria(empresa, usuario, TipoConvocatoria.PLANILLA_COMPLETA);
        c.setBusqueda(parada);
        c.publicar(usuario, HOY.atStartOfDay());
        when(convocatoriaRepository.findById(5L)).thenReturn(Optional.of(c));
        assertThat(service.mover(CORREO, 5L, null).busqueda()).isNull();
    }

    @Test
    void busquedaDeOtraEmpresa_esErrorDeCampo() {
        Empresa otra = mock(Empresa.class);
        when(otra.getId()).thenReturn(2L);
        when(busquedaRepository.findById(8L)).thenReturn(Optional.of(new Busqueda(otra, "Ajena")));
        assertThatThrownBy(() -> service.crear(CORREO, planilla(null, 8L)))
                .isInstanceOf(CamposInvalidosException.class)
                .satisfies(e -> assertThat(((CamposInvalidosException) e).getErrores()).containsKey("idBusqueda"));
        verify(convocatoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void convocatoriaDeOtraEmpresa_404() {
        Empresa otra = mock(Empresa.class);
        when(otra.getId()).thenReturn(2L);
        when(convocatoriaRepository.findById(6L))
                .thenReturn(Optional.of(new Convocatoria(otra, usuario, TipoConvocatoria.PLANILLA_COMPLETA)));
        assertThatThrownBy(() -> service.detalle(CORREO, 6L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.eliminar(CORREO, 6L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
