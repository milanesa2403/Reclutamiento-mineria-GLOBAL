package com.mineria.reclutamiento.config;

import com.mineria.reclutamiento.modelo.Rol;
import com.mineria.reclutamiento.modelo.Usuario;
import com.mineria.reclutamiento.repositorio.UsuarioRepositorio;
import com.mineria.reclutamiento.servicio.AuditoriaServicio;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SeguridadConfig {

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecretKey claveJwt(@Value("${jwt.secreto}") String secreto) {
        return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey claveJwt) { return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt)); }

    @Bean
    JwtDecoder jwtDecoder(SecretKey claveJwt) {
        return NimbusJwtDecoder.withSecretKey(claveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, AuditoriaServicio auditoria) throws Exception {
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(jwt ->
                List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString("rol"))));

        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/v1/auth/**", "/api/health", "/v1/health", "/v0/**",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/documentos").hasRole("TRABAJADOR")
                .requestMatchers("/v1/trabajador/**").hasRole("TRABAJADOR")
                .requestMatchers("/v1/empresa/**").hasRole("EMPRESA")
                .requestMatchers("/v1/admin/**").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conv)))
            .exceptionHandling(e -> e.accessDeniedHandler((req, res, ex) -> {
                var a = SecurityContextHolder.getContext().getAuthentication();
                UUID id = a instanceof JwtAuthenticationToken t ? UUID.fromString(t.getName()) : null;
                auditoria.registrar(id, "ACCESO_DENEGADO", req.getRequestURI(), false);
                res.setStatus(403);
                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                res.getWriter().write("{\"codigo\":\"ACCESO_DENEGADO\",\"mensaje\":\"Acceso denegado\"}");
            }));
        return http.build();
    }

    /** Solo desarrollo: crea un administrador al iniciar. */
    @Bean
    @Profile("!prod")
    CommandLineRunner adminInicial(UsuarioRepositorio repo, PasswordEncoder enc, @Value("${admin.clave}") String clave) {
        return args -> {
            if (!repo.existsByCorreo("admin@mineria.com")) {
                Usuario u = new Usuario(UUID.randomUUID(), "admin@mineria.com", Rol.ADMINISTRADOR);
                u.setClaveHash(enc.encode(clave));
                repo.save(u);
            }
        };
    }
}
