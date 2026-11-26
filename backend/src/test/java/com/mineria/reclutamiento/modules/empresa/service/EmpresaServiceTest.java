package com.mineria.reclutamiento.modules.empresa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mineria.reclutamiento.core.exception.DuplicateResourceException;
import com.mineria.reclutamiento.core.exception.ForbiddenException;
import com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa;
import java.util.List;
import com.mineria.reclutamiento.modules.auth.entity.Rol;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import com.mineria.reclutamiento.modules.empresa.dto.request.EmpresaPerfilUpdateRequest;
import com.mineria.reclutamiento.modules.empresa.dto.request.EmpresaRegistroRequest;
import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import com.mineria.reclutamiento.modules.empresa.entity.Representante;
import com.mineria.reclutamiento.modules.empresa.entity.SectorMinero;
import com.mineria.reclutamiento.modules.empresa.repository.EmpresaRepository;
import com.mineria.reclutamiento.modules.empresa.repository.RepresentanteRepository;
import com.mineria.reclutamiento.modules.persona.dto.IdentidadPersona;
import com.mineria.reclutamiento.modules.persona.entity.TipoDocumentoIdentidad;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.persona.service.PersonaService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmpresaServiceTest {

    private static final String CORREO = "empresa@prueba.com";

    @Mock EmpresaRepository empresaRepository;
    @Mock RepresentanteRepository representanteRepository;
    @Mock SectorMineroService sectorMineroService;
    @Mock PersonaService personaService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock ActividadEmpresaService actividad;

    EmpresaService service;
    Empresa empresa;
    Persona persona;
    Representante representante;

    @BeforeEach
    void setUp() {
        service = new EmpresaService(empresaRepository, representanteRepository, sectorMineroService,
                personaService, usuarioRepository, actividad);

        Usuario usuario = new Usuario(CORREO, Rol.EMPRESA);
        empresa = new Empresa("20123456789", "MINERA OFICIAL S.A.", "Av. Original 1", "014445555",
                "contacto@minera.pe", null);
        empresa.setDescripcion("Descripción original");
        empresa.setEstado(EstadoEmpresa.APROBADA);
        persona = new Persona(3L, usuario, "12345678", "ANA", "RUIZ", "987654321", "Calle Rep 2");
        representante = new Representante(1L, persona, empresa, "Jefe de RRHH");

        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(personaService.obtenerPorUsuarioId(any())).thenReturn(persona);
        when(representanteRepository.findByPersona_IdPersona(3L)).thenReturn(Optional.of(representante));
    }

    @Test
    void perfil_camposOmitidosNoSeBorran() {
        EmpresaPerfilUpdateRequest request = new EmpresaPerfilUpdateRequest("Nueva descripción", null, null, null, null, null);

        service.actualizarPerfil(CORREO, request);

        assertThat(empresa.getDescripcion()).isEqualTo("Nueva descripción");
        assertThat(empresa.getDireccion()).isEqualTo("Av. Original 1");
        assertThat(empresa.getTelefono()).isEqualTo("014445555");
        assertThat(empresa.getCorreoCorporativo()).isEqualTo("contacto@minera.pe");
        assertThat(persona.getTelefono()).isEqualTo("987654321");
        assertThat(representante.getCargo()).isEqualTo("Jefe de RRHH");
        verifyNoInteractions(sectorMineroService);
    }

    @Test
    void perfil_empresaNoAprobada_soloPuedeVerlo_403() {
        for (EstadoEmpresa estado : List.of(EstadoEmpresa.PENDIENTE_DOCUMENTO, EstadoEmpresa.EN_REVISION,
                EstadoEmpresa.OBSERVADA, EstadoEmpresa.RECHAZADA)) {
            empresa.setEstado(estado);
            EmpresaPerfilUpdateRequest request = new EmpresaPerfilUpdateRequest("Cambio no permitido", null, null, null, null, null);

            assertThatThrownBy(() -> service.actualizarPerfil(CORREO, request))
                    .as("estado %s", estado)
                    .isInstanceOf(ForbiddenException.class);
            assertThat(empresa.getDescripcion()).isEqualTo("Descripción original");
            assertThat(service.perfilDe(CORREO).estado()).isEqualTo(estado.name());
        }
    }

    @Test
    void perfil_textoVacioBorraSoloCamposOpcionales() {
        EmpresaPerfilUpdateRequest request = new EmpresaPerfilUpdateRequest("   ", "", null, null, null, null);

        service.actualizarPerfil(CORREO, request);

        assertThat(empresa.getDescripcion()).isNull();
        assertThat(empresa.getTelefono()).isNull();
    }

    @Test
    void perfil_nuncaCambiaDatosOficialesNiCorreoCorporativo() {
        EmpresaPerfilUpdateRequest request = new EmpresaPerfilUpdateRequest(null, null, null, null, null, " Gerente ");

        service.actualizarPerfil(CORREO, request);

        assertThat(empresa.getRuc()).isEqualTo("20123456789");
        assertThat(empresa.getRazonSocial()).isEqualTo("MINERA OFICIAL S.A.");
        assertThat(empresa.getDireccion()).isEqualTo("Av. Original 1");
        assertThat(empresa.getCorreoCorporativo()).isEqualTo("contacto@minera.pe");
        assertThat(representante.getCargo()).isEqualTo("Gerente");
    }

    @Test
    void perfil_telefonoDeOtraEmpresa_409() {
        when(empresaRepository.existsByTelefonoAndIdNot("015556666", null)).thenReturn(true);
        EmpresaPerfilUpdateRequest request = new EmpresaPerfilUpdateRequest(null, "015556666", null, null, null, null);

        assertThatThrownBy(() -> service.actualizarPerfil(CORREO, request))
                .isInstanceOf(DuplicateResourceException.class);
        assertThat(empresa.getTelefono()).isEqualTo("014445555");
    }

    @Test
    void perfil_telefonoRepresentanteDeOtraPersona_409() {
        when(personaService.telefonoExistente("900000000", 3L)).thenReturn(true);
        EmpresaPerfilUpdateRequest request = new EmpresaPerfilUpdateRequest(null, null, null, "900000000", null, null);

        assertThatThrownBy(() -> service.actualizarPerfil(CORREO, request))
                .isInstanceOf(DuplicateResourceException.class);
        assertThat(persona.getTelefono()).isEqualTo("987654321");
    }


    @Test
    void registro_usaLosDatosDelFormulario_yTelefonoVacioQuedaNull() {
        Usuario usuario = new Usuario(CORREO, Rol.EMPRESA);
        IdentidadPersona dniRepresentante = new IdentidadPersona("PE", TipoDocumentoIdentidad.DNI, "12345678", "ANA", "RUIZ PAZ");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(sectorMineroService.obtenerActivo(2L)).thenReturn(mock(SectorMinero.class));
        when(personaService.crearPersonaOficial(dniRepresentante, "987654321", null, usuario))
                .thenReturn(persona);

        EmpresaRegistroRequest request = new EmpresaRegistroRequest("pe", " 20123456789 ", " MINERA REAL S.A.C. ",
                "AV. SUNAT 123, LIMA", "   ", 2L, "PE", TipoDocumentoIdentidad.DNI, "12345678", "ANA", "RUIZ PAZ",
                "987654321", null, "Gerente");

        service.registrarDatos(1L, CORREO, request, dniRepresentante);

        ArgumentCaptor<Empresa> guardada = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).save(guardada.capture());
        assertThat(guardada.getValue().getRazonSocial()).isEqualTo("MINERA REAL S.A.C.");
        assertThat(guardada.getValue().getPais()).isEqualTo("PE");
        assertThat(guardada.getValue().getRuc()).isEqualTo("20123456789");
        assertThat(guardada.getValue().getTelefono()).isNull();
        assertThat(guardada.getValue().getDireccion()).isEqualTo("AV. SUNAT 123, LIMA");
        verify(personaService).crearPersonaOficial(dniRepresentante, "987654321", null, usuario);
    }
}
