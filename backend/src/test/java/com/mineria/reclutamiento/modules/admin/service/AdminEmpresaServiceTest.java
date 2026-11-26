package com.mineria.reclutamiento.modules.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.modules.auth.entity.Rol;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import com.mineria.reclutamiento.modules.documento.service.DocumentoService;
import com.mineria.reclutamiento.core.config.AwsProperties;
import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa;
import com.mineria.reclutamiento.modules.empresa.entity.Representante;
import com.mineria.reclutamiento.modules.empresa.repository.HistorialRechazoRepository;
import com.mineria.reclutamiento.modules.empresa.repository.EmpresaRepository;
import com.mineria.reclutamiento.modules.empresa.repository.RepresentanteRepository;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.empresa.entity.TipoActividadEmpresa;
import com.mineria.reclutamiento.modules.empresa.service.ActividadEmpresaService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminEmpresaServiceTest {

    private static final String ADMIN = "admin@prueba.com";

    @Mock EmpresaRepository empresaRepository;
    @Mock RepresentanteRepository representanteRepository;
    @Mock HistorialRechazoRepository historialRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock DocumentoService documentoService;
    @Mock PlatformTransactionManager txManager;
    @Mock ActividadEmpresaService actividad;

    AdminEmpresaService service;
    Empresa empresa;
    Usuario admin;

    @BeforeEach
    void setUp() {
        service = new AdminEmpresaService(empresaRepository, representanteRepository, historialRepository,
                usuarioRepository, documentoService,
                new AwsProperties("us-east-2", "bucket-prueba", "", "cvs", 300), txManager, actividad);

        admin = new Usuario(ADMIN, Rol.ADMINISTRADOR);
        empresa = new Empresa("20123456789", "MINERA REAL S.A.C.", null, null, "empresa@prueba.com", null);
        Usuario cuentaEmpresa = new Usuario("empresa@prueba.com", Rol.EMPRESA);
        Persona persona = new Persona(3L, cuentaEmpresa, "12345678", "ANA", "RUIZ", "987654321", null);
        Representante representante = new Representante(1L, persona, empresa, "Gerente");

        when(empresaRepository.findById(10L)).thenReturn(Optional.of(empresa));
        when(usuarioRepository.findByCorreo(ADMIN)).thenReturn(Optional.of(admin));
        when(representanteRepository.findByEmpresa_Id(any())).thenReturn(List.of(representante));
        when(historialRepository.findByRucOrderByFechaRechazoDesc(anyString())).thenReturn(List.of());
    }

    @Test
    void aprobar_soloDesdeEnRevision_yRegistraLaDecision() {
        empresa.setEstado(EstadoEmpresa.EN_REVISION);

        var res = service.aprobar(10L, ADMIN);

        assertThat(res.estado()).isEqualTo(EstadoEmpresa.APROBADA);
        assertThat(empresa.getAdminRevisor()).isSameAs(admin);
        assertThat(empresa.getFechaRevision()).isNotNull();
        verify(actividad).registrar(empresa, TipoActividadEmpresa.APROBADA, null, ADMIN);
    }

    @Test
    void noSePuedeAprobarSinDocumentoNiDosVeces() {
        for (EstadoEmpresa estado : List.of(EstadoEmpresa.PENDIENTE_DOCUMENTO, EstadoEmpresa.OBSERVADA,
                EstadoEmpresa.APROBADA, EstadoEmpresa.RECHAZADA)) {
            empresa.setEstado(estado);
            assertThatThrownBy(() -> service.aprobar(10L, ADMIN)).as("estado %s", estado)
                    .isInstanceOf(BadRequestException.class);
        }
        verify(actividad, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void observar_guardaElMotivo_yPermiteQueLaEmpresaVuelvaASubir() {
        empresa.setEstado(EstadoEmpresa.EN_REVISION);

        var res = service.observar(10L, "  El documento está ilegible  ", ADMIN);

        assertThat(res.estado()).isEqualTo(EstadoEmpresa.OBSERVADA);
        assertThat(empresa.getMotivoRevision()).isEqualTo("El documento está ilegible");
        verify(actividad).registrar(empresa, TipoActividadEmpresa.OBSERVADA, "El documento está ilegible", ADMIN);
    }

    @Test
    void rechazar_desdePendienteEnRevisionUObservada_peroNoUnaYaAprobada() {
        for (EstadoEmpresa estado : List.of(EstadoEmpresa.PENDIENTE_DOCUMENTO, EstadoEmpresa.EN_REVISION,
                EstadoEmpresa.OBSERVADA)) {
            empresa.setEstado(estado);
            assertThat(service.rechazar(10L, "RUC no corresponde", ADMIN).estado()).isEqualTo(EstadoEmpresa.RECHAZADA);
        }
        empresa.setEstado(EstadoEmpresa.APROBADA);
        assertThatThrownBy(() -> service.rechazar(10L, "x", ADMIN)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void listar_filtraPorEstado() {
        empresa.setEstado(EstadoEmpresa.EN_REVISION);
        when(empresaRepository.findByEstadoOrderByIdDesc(EstadoEmpresa.EN_REVISION)).thenReturn(List.of(empresa));

        var lista = service.listar(EstadoEmpresa.EN_REVISION);

        assertThat(lista).singleElement().satisfies(i -> {
            assertThat(i.ruc()).isEqualTo("20123456789");
            assertThat(i.estado()).isEqualTo(EstadoEmpresa.EN_REVISION);
            assertThat(i.tieneDocumento()).isFalse();
        });
        verify(empresaRepository, never()).findAllByOrderByIdDesc();
    }

    @Test
    void detalle_muestraAlSolicitanteConSusDatosOficiales() {
        var detalle = service.detalle(10L);
        assertThat(detalle.solicitantes()).singleElement().satisfies(s -> {
            assertThat(s.dni()).isEqualTo("12345678");
            assertThat(s.correo()).isEqualTo("empresa@prueba.com");
            assertThat(s.cargo()).isEqualTo("Gerente");
        });
    }
}
