package com.mineria.reclutamiento.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mineria.reclutamiento.core.config.CognitoProperties;
import com.mineria.reclutamiento.modules.auth.entity.Rol;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class CognitoAutenticacionTest {

    final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    final CognitoAutenticacion conversor = new CognitoAutenticacion(usuarios);

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("t").header("alg", "RS256").claim("email", " Ana@Prueba.PE ");
    }

    @Test
    void elNombreEsElCorreo_yElRolSaleDeLaBase() {
        when(usuarios.findByCorreo("ana@prueba.pe")).thenReturn(Optional.of(new Usuario("ana@prueba.pe", Rol.POSTULANTE)));

        var auth = conversor.convert(token().build());

        assertThat(auth.getName()).isEqualTo("ana@prueba.pe");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_POSTULANTE");
    }

    @Test
    void sinRegistro_oCuentaInactiva_noTieneRol() {
        when(usuarios.findByCorreo("ana@prueba.pe")).thenReturn(Optional.empty());
        assertThat(conversor.convert(token().build()).getAuthorities()).isEmpty();

        Usuario inactivo = new Usuario("ana@prueba.pe", Rol.EMPRESA);
        inactivo.setCuentaActiva(false);
        when(usuarios.findByCorreo("ana@prueba.pe")).thenReturn(Optional.of(inactivo));
        assertThat(conversor.convert(token().build()).getAuthorities()).isEmpty();
    }

    @Test
    void grupoAdmin_daRolDeAdministrador() {
        var auth = conversor.convert(token().claim("cognito:groups", List.of("ADMIN")).build());
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMINISTRADOR");
    }

    @Test
    void soloSeAceptanTokensDeIdentidadDeEsteCliente() {
        var validador = SecurityConfig.tokenDeIdentidad("cliente");
        assertThat(validador.validate(Jwt.withTokenValue("t").header("alg", "RS256")
                .claim("token_use", "id").audience(List.of("cliente")).build()).hasErrors()).isFalse();
        assertThat(validador.validate(Jwt.withTokenValue("t").header("alg", "RS256")
                .claim("token_use", "access").audience(List.of("cliente")).build()).hasErrors()).isTrue();
        assertThat(validador.validate(Jwt.withTokenValue("t").header("alg", "RS256")
                .claim("token_use", "id").audience(List.of("otro")).build()).hasErrors()).isTrue();
    }

    @Test
    void laConfiguracionPublicaSaleDelIssuer() {
        var props = new CognitoProperties("https://cognito-idp.us-east-2.amazonaws.com/us-east-2_AbC123", "cli");
        assertThat(props.userPoolId()).isEqualTo("us-east-2_AbC123");
        assertThat(props.region()).isEqualTo("us-east-2");
        assertThat(new CognitoProperties("", "cli").userPoolId()).isEmpty();
        assertThat(new CognitoProperties(null, null).region()).isEmpty();
    }
}
