package com.mineria.reclutamiento.modules.postulante.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mineria.reclutamiento.core.exception.DuplicateResourceException;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.auth.entity.Rol;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import com.mineria.reclutamiento.modules.persona.dto.IdentidadPersona;
import com.mineria.reclutamiento.modules.persona.entity.TipoDocumentoIdentidad;
import com.mineria.reclutamiento.modules.persona.entity.Persona;
import com.mineria.reclutamiento.modules.persona.repository.PersonaRepository;
import com.mineria.reclutamiento.modules.persona.service.PersonaService;
import com.mineria.reclutamiento.modules.postulante.dto.request.ActualizarPostulanteRequest;
import com.mineria.reclutamiento.modules.postulante.dto.request.PostulanteRequest;
import com.mineria.reclutamiento.modules.postulante.dto.response.PerfilPostulanteResponse;
import com.mineria.reclutamiento.modules.postulante.entity.Postulante;
import com.mineria.reclutamiento.modules.postulante.repository.PostulanteRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostulanteServiceTest {

    private static final String CORREO = "postulante@prueba.com";

    @Mock PostulanteRepository postulanteRepository;
    @Mock PersonaRepository personaRepository;
    @Mock PersonaService personaService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock EstadoPerfilProfesional estadoPerfil;

    PostulanteService service;

    @BeforeEach
    void setUp() {
        service = new PostulanteService(postulanteRepository, personaRepository, personaService, usuarioRepository,
                estadoPerfil);
    }


    @Test
    void registro_usaLosNombresValidadosEIgnoraLasUrls() {
        Usuario usuario = new Usuario(CORREO, Rol.POSTULANTE);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(personaRepository.save(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));

        PostulanteRequest request = new PostulanteRequest("BO", TipoDocumentoIdentidad.CEDULA, "1234567",
                "NOMBRE FALSO", "APELLIDO FALSO", " 987654321 ", null);

        service.registrarDatos(1L, request,
                new IdentidadPersona("BO", TipoDocumentoIdentidad.CEDULA, "1234567", "JUAN CARLOS", "PEREZ GOMEZ"));

        ArgumentCaptor<Persona> persona = ArgumentCaptor.forClass(Persona.class);
        verify(personaRepository).save(persona.capture());
        assertThat(persona.getValue().getNombres()).isEqualTo("JUAN CARLOS");
        assertThat(persona.getValue().getPais()).isEqualTo("BO");
        assertThat(persona.getValue().getTipoDocumento()).isEqualTo(TipoDocumentoIdentidad.CEDULA);
        assertThat(persona.getValue().getApellidos()).isEqualTo("PEREZ GOMEZ");
        assertThat(persona.getValue().getTelefono()).isEqualTo("987654321");

        ArgumentCaptor<Postulante> postulante = ArgumentCaptor.forClass(Postulante.class);
        verify(postulanteRepository).save(postulante.capture());
        assertThat(postulante.getValue().getUrlCv()).isNull();
        assertThat(postulante.getValue().getUrlFotoDni()).isNull();
        assertThat(postulante.getValue().getUrlCul()).isNull();
    }


    @Test
    void miPerfil_actualizaSoloTelefonoDireccionYResumen() {
        Persona persona = prepararPerfil();
        when(personaRepository.existsByTelefonoAndIdPersonaNot("911222333", 10L)).thenReturn(false);

        ActualizarPostulanteRequest request =
                new ActualizarPostulanteRequest(" 911222333 ", "   ", "  Operador de maquinaria pesada  ");

        PerfilPostulanteResponse res = service.actualizarMiPerfil(CORREO, request);

        assertThat(persona.getTelefono()).isEqualTo("911222333");
        assertThat(persona.getDireccion()).isNull();
        assertThat(res.resumenProfesional()).isEqualTo("Operador de maquinaria pesada");
        assertThat(res.dni()).isEqualTo("12345678");
        assertThat(res.nombres()).isEqualTo("JUAN");
        assertThat(res.apellidos()).isEqualTo("PEREZ");
        assertThat(res.correo()).isEqualTo(CORREO);
    }

    @Test
    void miPerfil_telefonoDeOtraPersona_409_sinCambios() {
        Persona persona = prepararPerfil();
        when(personaRepository.existsByTelefonoAndIdPersonaNot("900000000", 10L)).thenReturn(true);

        ActualizarPostulanteRequest request = new ActualizarPostulanteRequest("900000000", null, null);

        assertThatThrownBy(() -> service.actualizarMiPerfil(CORREO, request))
                .isInstanceOf(DuplicateResourceException.class);
        assertThat(persona.getTelefono()).isEqualTo("987654321");
    }

    @Test
    void miPerfil_cuentaSinPerfilDePostulante_404() {
        Usuario usuario = new Usuario(CORREO, Rol.EMPRESA);
        Persona persona = new Persona(10L, usuario, "12345678", "JUAN", "PEREZ", "987654321", null);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(personaRepository.findByUsuarioId(any())).thenReturn(Optional.of(persona));
        when(postulanteRepository.findByPersonaIdPersona(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerMiPerfil(CORREO)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void miPerfil_buscaSiempreAPartirDelCorreoDelToken() {
        prepararPerfil();
        PerfilPostulanteResponse res = service.obtenerMiPerfil(CORREO);
        assertThat(res.dni()).isEqualTo("12345678");
        verify(usuarioRepository).findByCorreo(CORREO);
        verify(postulanteRepository, never()).findById(anyLong());
    }

    private Persona prepararPerfil() {
        Usuario usuario = new Usuario(CORREO, Rol.POSTULANTE);
        org.springframework.test.util.ReflectionTestUtils.setField(usuario, "id", 7L);
        Persona persona = new Persona(10L, usuario, "12345678", "JUAN", "PEREZ", "987654321", "Calle 1");
        Postulante postulante = new Postulante();
        postulante.setPersona(persona);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(personaRepository.findByUsuarioId(any())).thenReturn(Optional.of(persona));
        when(postulanteRepository.findByPersonaIdPersona(10L)).thenReturn(Optional.of(postulante));
        return persona;
    }
}
