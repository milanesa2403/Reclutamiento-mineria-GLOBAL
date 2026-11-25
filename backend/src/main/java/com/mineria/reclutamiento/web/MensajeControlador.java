package com.mineria.reclutamiento.web;

import com.mineria.reclutamiento.modelo.Mensaje;
import com.mineria.reclutamiento.servicio.MensajeServicio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/postulaciones/{postulacionId}/mensajes")
public class MensajeControlador {
    public record MensajePeticion(@NotBlank @Size(max = 2000) String contenido) {}

    private final MensajeServicio mensajes;

    public MensajeControlador(MensajeServicio mensajes) { this.mensajes = mensajes; }

    @GetMapping
    public List<Mensaje> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID postulacionId) {
        return mensajes.listar(Actual.id(jwt), postulacionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mensaje enviar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID postulacionId,
            @RequestHeader("Idempotency-Key") String clave, @Valid @RequestBody MensajePeticion p) {
        return mensajes.enviar(clave, postulacionId, Actual.id(jwt), p.contenido());
    }
}
