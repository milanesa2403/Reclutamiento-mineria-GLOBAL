package com.mineria.reclutamiento.core.security;

import com.mineria.reclutamiento.modules.auth.entity.Rol;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CognitoAutenticacion implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String GRUPO_ADMIN = "ADMIN";

    private final UsuarioRepository usuarioRepository;

    public CognitoAutenticacion(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String correo = correoDe(jwt);
        List<GrantedAuthority> roles = esAdministrador(jwt)
                ? List.of(new SimpleGrantedAuthority("ROLE_" + Rol.ADMINISTRADOR.name()))
                : usuarioRepository.findByCorreo(correo)
                        .filter(Usuario::isCuentaActiva)
                        .<List<GrantedAuthority>>map(u -> List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name())))
                        .orElse(List.of());
        return new JwtAuthenticationToken(jwt, roles, correo);
    }

    public static boolean esAdministrador(Jwt jwt) {
        List<String> grupos = jwt.getClaimAsStringList("cognito:groups");
        return grupos != null && grupos.contains(GRUPO_ADMIN);
    }

    public static String correoDe(Jwt jwt) {
        String correo = jwt.getClaimAsString("email");
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }
}
