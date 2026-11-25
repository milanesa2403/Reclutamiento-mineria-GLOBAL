package com.mineria.reclutamiento.web;

import com.mineria.reclutamiento.modelo.*;
import com.mineria.reclutamiento.servicio.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/trabajador")
public class TrabajadorControlador {
    public record PerfilPeticion(@NotBlank String nombreCompleto, @NotBlank String pais, @NotBlank String idioma,
                                 String especialidad, @Min(0) @Max(70) int aniosExperiencia) {}
    public record PostulacionPeticion(@NotNull UUID ofertaId, @NotNull UUID cvId) {}

    private final PerfilServicio perfiles;
    private final PostulacionServicio postulaciones;
    private final DocumentoServicio documentos;
    private final ColaPostulaciones cola;
    private final EstadoSistema estado;

    public TrabajadorControlador(PerfilServicio perfiles, PostulacionServicio postulaciones, DocumentoServicio documentos,
            ColaPostulaciones cola, EstadoSistema estado) {
        this.perfiles = perfiles;
        this.postulaciones = postulaciones;
        this.documentos = documentos;
        this.cola = cola;
        this.estado = estado;
    }

    @GetMapping("/perfil")
    public Perfil perfil(@AuthenticationPrincipal Jwt jwt) { return perfiles.obtener(Actual.id(jwt)); }

    @PutMapping("/perfil")
    public Perfil actualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PerfilPeticion p) {
        return perfiles.actualizar(Actual.id(jwt), new PerfilServicio.DatosPerfil(
                p.nombreCompleto(), p.pais(), p.idioma(), p.especialidad(), p.aniosExperiencia()));
    }

    @GetMapping("/documentos")
    public List<Documento> misDocumentos(@AuthenticationPrincipal Jwt jwt) { return documentos.listar(Actual.id(jwt)); }

    /** Header Idempotency-Key obligatorio: repetir la peticion no duplica la postulacion. */
    @PostMapping("/postulaciones")
    public ResponseEntity<?> postular(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String clave, @Valid @RequestBody PostulacionPeticion p) {
        UUID yo = Actual.id(jwt);
        if (estado.isSoloLectura()) {
            cola.encolar(clave, p.ofertaId(), yo, p.cvId());
            Map<String, String> m = new LinkedHashMap<>();
            m.put("estado", "EN_PROCESO");
            m.put("mensaje", "Su postulacion quedo en proceso y se registrara en cuanto el sistema vuelva a operar.");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(m);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(postulaciones.postular(clave, p.ofertaId(), yo, p.cvId()));
    }

    @GetMapping("/postulaciones")
    public List<Postulacion> misPostulaciones(@AuthenticationPrincipal Jwt jwt) {
        return postulaciones.listarDeTrabajador(Actual.id(jwt));
    }
}
