package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.modelo.Usuario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class TokenServicio {
    private final JwtEncoder encoder;

    public TokenServicio(JwtEncoder encoder) { this.encoder = encoder; }

    public String generar(Usuario u) {
        Instant ahora = Instant.now();
        long minutos=0;
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(u.getId().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(minutos, ChronoUnit.MINUTES))
                .claim("rol", u.getRol().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
