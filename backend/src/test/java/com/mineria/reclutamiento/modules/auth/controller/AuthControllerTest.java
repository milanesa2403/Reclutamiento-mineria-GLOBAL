package com.mineria.reclutamiento.modules.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mineria.reclutamiento.modules.auth.entity.Rol;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import com.mineria.reclutamiento.modules.empresa.dto.request.EmpresaRegistroRequest;
import com.mineria.reclutamiento.modules.empresa.repository.EmpresaRepository;
import com.mineria.reclutamiento.modules.empresa.service.EmpresaService;
import com.mineria.reclutamiento.modules.persona.dto.IdentidadPersona;
import com.mineria.reclutamiento.modules.persona.entity.TipoDocumentoIdentidad;
import com.mineria.reclutamiento.modules.persona.repository.PersonaRepository;
import com.mineria.reclutamiento.modules.postulante.dto.request.PostulanteRequest;
import com.mineria.reclutamiento.modules.postulante.service.PostulanteService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {

    private static final String CORREO = "ana@prueba.pe";

    @Mock UsuarioRepository usuarioRepository;
    @Mock EmpresaService empresaService;
    @Mock PostulanteService postulanteService;
    @Mock EmpresaRepository empresaRepository;
    @Mock PersonaRepository personaRepository;
    @Mock PlatformTransactionManager txManager;

    AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(usuarioRepository, empresaService, postulanteService, empresaRepository,
                personaRepository, txManager);
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 1L);
            return u;
        });
    }

    private static Jwt token(String correo, String... grupos) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "RS256").claim("email", correo);
        if (grupos.length > 0) {
            b.claim("cognito:groups", List.of(grupos));
        }
        return b.build();
    }

    private static PostulanteRequest postulante() {
        return postulante("pe", TipoDocumentoIdentidad.DNI, "12345678");
    }

    private static PostulanteRequest postulante(String pais, TipoDocumentoIdentidad tipo, String numero) {
        return new PostulanteRequest(pais, tipo, numero, " Ana ", " Ruiz ", " 987654321 ", "Operadora");
    }

    private static EmpresaRegistroRequest empresa() {
        return empresa("PE", "20123456789");
    }

    private static EmpresaRegistroRequest empresa(String pais, String identificador) {
        return new EmpresaRegistroRequest(pais, identificador, " Minera Andina S.A.C. ", "Av. Lima 1", "014445555", 2L,
                "PE", TipoDocumentoIdentidad.DNI, "87654321", "Luis", "Paz", "999888777", null, "Gerente");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> datos(Object cuerpo) {
        return (Map<String, Object>) ((Map<String, Object>) cuerpo).get("data");
    }

    @Test
    void yo_sinRegistro_devuelveRolNulo() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        var res = controller.yo(token("Ana@Prueba.pe"));

        assertThat(datos(res.getBody())).containsEntry("correo", CORREO).containsEntry("rol", null);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void yo_conRegistro_devuelveElRolDeLaBase() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(new Usuario(CORREO, Rol.EMPRESA)));

        assertThat(datos(controller.yo(token(CORREO)).getBody())).containsEntry("rol", "EMPRESA");
    }

    @Test
    void yo_grupoAdminDeCognito_creaElAdministrador() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        var res = controller.yo(token(CORREO, "ADMIN"));

        assertThat(datos(res.getBody())).containsEntry("rol", "ADMINISTRADOR");
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getRol()).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(guardado.getValue().isCuentaActiva()).isTrue();
    }

    @Test
    void registroPostulante_usaElCorreoDelToken_yLosNombresDelFormulario() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        var res = controller.registrarPostulante(token(CORREO), postulante());

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuario.capture());
        assertThat(usuario.getValue().getCorreo()).isEqualTo(CORREO);
        assertThat(usuario.getValue().getRol()).isEqualTo(Rol.POSTULANTE);
        verify(postulanteService).registrarDatos(eq(1L), any(), eq(new IdentidadPersona("PE", TipoDocumentoIdentidad.DNI, "12345678", "Ana", "Ruiz")));
    }

    @Test
    void registroPostulante_rechazaDuplicados() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(new Usuario(CORREO, Rol.POSTULANTE)));
        assertThat(controller.registrarPostulante(token(CORREO), postulante()).getStatusCode().value()).isEqualTo(400);

        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());
        when(personaRepository.existsByPaisAndTipoDocumentoAndDni("PE", TipoDocumentoIdentidad.DNI, "12345678")).thenReturn(true);
        assertThat(controller.registrarPostulante(token(CORREO), postulante()).getStatusCode().value()).isEqualTo(400);

        when(personaRepository.existsByPaisAndTipoDocumentoAndDni("PE", TipoDocumentoIdentidad.DNI, "12345678")).thenReturn(false);
        when(personaRepository.existsByTelefono("987654321")).thenReturn(true);
        assertThat(controller.registrarPostulante(token(CORREO), postulante()).getStatusCode().value()).isEqualTo(400);

        verify(postulanteService, never()).registrarDatos(any(), any(), any());
    }

    @Test
    void registroEmpresa_creaLaCuentaConLosDatosDelFormulario() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        var res = controller.registrarEmpresa(token(CORREO), empresa());

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        verify(empresaService).registrarDatos(eq(1L), eq(CORREO), any(),
                eq(new IdentidadPersona("PE", TipoDocumentoIdentidad.DNI, "87654321", "Luis", "Paz")));
    }

    @Test
    void registroEmpresa_rechazaDuplicados() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(new Usuario(CORREO, Rol.EMPRESA)));
        assertThat(controller.registrarEmpresa(token(CORREO), empresa()).getStatusCode().value()).isEqualTo(400);

        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());
        when(empresaService.existeIdentificadorFiscal("PE", "20123456789")).thenReturn(true);
        assertThat(controller.registrarEmpresa(token(CORREO), empresa()).getStatusCode().value()).isEqualTo(400);

        when(empresaService.existeIdentificadorFiscal("PE", "20123456789")).thenReturn(false);
        when(empresaService.existeDocumentoRepresentante(new IdentidadPersona("PE", TipoDocumentoIdentidad.DNI, "87654321", "Luis", "Paz"))).thenReturn(true);
        assertThat(controller.registrarEmpresa(token(CORREO), empresa()).getStatusCode().value()).isEqualTo(400);

        when(empresaService.existeDocumentoRepresentante(new IdentidadPersona("PE", TipoDocumentoIdentidad.DNI, "87654321", "Luis", "Paz"))).thenReturn(false);
        when(empresaRepository.existsByTelefono("014445555")).thenReturn(true);
        assertThat(controller.registrarEmpresa(token(CORREO), empresa()).getStatusCode().value()).isEqualTo(400);

        when(empresaRepository.existsByTelefono("014445555")).thenReturn(false);
        when(personaRepository.existsByTelefono(anyString())).thenReturn(true);
        assertThat(controller.registrarEmpresa(token(CORREO), empresa()).getStatusCode().value()).isEqualTo(400);

        verify(empresaService, never()).registrarDatos(any(), any(), any(), any());
    }

    @Test
    void registroPostulante_aceptaDocumentosDeOtrosPaises() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        var res = controller.registrarPostulante(token(CORREO), postulante("cl", TipoDocumentoIdentidad.CEDULA, "12.345.678-k"));

        assertThat(res.getStatusCode().value()).isEqualTo(400);
        res = controller.registrarPostulante(token(CORREO), postulante("cl", TipoDocumentoIdentidad.CEDULA, "12345678-k"));
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        verify(postulanteService).registrarDatos(eq(1L), any(),
                eq(new IdentidadPersona("CL", TipoDocumentoIdentidad.CEDULA, "12345678-K", "Ana", "Ruiz")));
    }

    @Test
    void registroPostulante_validaPaisYFormatoDelDocumento() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        var sinPais = controller.registrarPostulante(token(CORREO), postulante("Peru", TipoDocumentoIdentidad.DNI, "12345678"));
        var dniCorto = controller.registrarPostulante(token(CORREO), postulante("PE", TipoDocumentoIdentidad.DNI, "1234567"));
        var pasaporte = controller.registrarPostulante(token(CORREO), postulante("PE", TipoDocumentoIdentidad.PASAPORTE, "AB12345"));

        assertThat(sinPais.getStatusCode().value()).isEqualTo(400);
        assertThat(((Map<?, ?>) sinPais.getBody()).containsKey("pais")).isTrue();
        assertThat(dniCorto.getStatusCode().value()).isEqualTo(400);
        assertThat(((Map<?, ?>) dniCorto.getBody()).containsKey("numeroDocumento")).isTrue();
        assertThat(pasaporte.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void registroEmpresa_aplicaLaReglaDelRucSoloEnPeru() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        var rucInvalido = controller.registrarEmpresa(token(CORREO), empresa("PE", "10123456789"));
        var nitColombia = controller.registrarEmpresa(token(CORREO), empresa("CO", "900123456-7"));
        var vacio = controller.registrarEmpresa(token(CORREO), empresa("CO", "12"));

        assertThat(rucInvalido.getStatusCode().value()).isEqualTo(400);
        assertThat(((Map<?, ?>) rucInvalido.getBody()).containsKey("identificadorFiscal")).isTrue();
        assertThat(nitColombia.getStatusCode().value()).isEqualTo(200);
        assertThat(vacio.getStatusCode().value()).isEqualTo(400);
    }
}
