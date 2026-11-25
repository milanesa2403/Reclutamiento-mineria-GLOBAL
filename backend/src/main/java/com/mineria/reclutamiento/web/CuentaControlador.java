package com.mineria.reclutamiento.web;

import com.mineria.reclutamiento.servicio.PrivacidadServicio;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Derecho de supresion (Ley 29733 / RGPD). */
@RestController
@RequestMapping("/v1/cuenta")
public class CuentaControlador {
    private final PrivacidadServicio privacidad;

    public CuentaControlador(PrivacidadServicio privacidad) { this.privacidad = privacidad; }

    @PostMapping("/solicitud-borrado")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> solicitarBorrado(@AuthenticationPrincipal Jwt jwt) {
        privacidad.solicitarBorrado(Actual.id(jwt));
        return Map.of("mensaje", "Solicitud registrada. Sus datos seran eliminados en un maximo de 30 dias.");
    }
}
