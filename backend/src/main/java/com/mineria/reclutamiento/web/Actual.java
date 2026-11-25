package com.mineria.reclutamiento.web;

import com.mineria.reclutamiento.modelo.Rol;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Datos del usuario autenticado, tomados del JWT ya validado. */
final class Actual {
    private Actual() {}
    static UUID id(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    static Rol rol(Jwt jwt) { return Rol.valueOf(jwt.getClaimAsString("rol")); }
}
