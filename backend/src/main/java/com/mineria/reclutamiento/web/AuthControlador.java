package com.mineria.reclutamiento.web;

import com.mineria.reclutamiento.modelo.Rol;
import com.mineria.reclutamiento.modelo.Usuario;
import com.mineria.reclutamiento.servicio.AutenticacionServicio;
import com.mineria.reclutamiento.servicio.TokenServicio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
public class AuthControlador {
    public record RegistroPeticion(
            @NotBlank @Email String correo,
            @NotBlank @Size(min = 12, message = "La clave debe tener al menos 12 caracteres") String clave,
            @NotNull Rol rol,
            @NotBlank String nombre,
            @NotBlank @Size(min = 2, max = 2, message = "El pais debe ser ISO 3166-1 (2 letras)") String pais,
            @NotBlank @Size(min = 2, max = 2, message = "El idioma debe ser ISO 639-1 (2 letras)") String idioma,
            @AssertTrue(message = "Debe aceptar el tratamiento de datos personales") boolean consentimiento) {}

    public record LoginPeticion(@NotBlank String correo, @NotBlank String clave, String codigoTotp) {}

    private final AutenticacionServicio auth;
    private final TokenServicio tokens;

    public AuthControlador(AutenticacionServicio auth, TokenServicio tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registro(@Valid @RequestBody RegistroPeticion p) {
        Usuario u = auth.registrar(p.correo(), p.clave(), p.rol(), p.nombre(), p.pais(), p.idioma(), p.consentimiento());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", u.getId(), "correo", u.getCorreo(), "rol", u.getRol()));
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginPeticion p) {
        Usuario u = auth.iniciarSesion(p.correo(), p.clave(), p.codigoTotp());
        return Map.of("token", tokens.generar(u), "tipo", "Bearer", "rol", u.getRol().name());
    }
}
