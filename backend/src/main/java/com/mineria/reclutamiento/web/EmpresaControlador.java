package com.mineria.reclutamiento.web;

import com.mineria.reclutamiento.modelo.*;
import com.mineria.reclutamiento.servicio.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/empresa")
public class EmpresaControlador {
    public record OfertaPeticion(@NotBlank String titulo, String descripcion, String especialidadRequerida,
                                 @NotBlank String pais, @PositiveOrZero double salario, @NotBlank String moneda) {}
    public record EstadoPeticion(@NotNull EstadoPostulacion estado) {}

    private final OfertaServicio ofertas;
    private final PostulacionServicio postulaciones;
    private final PerfilServicio perfiles;
    private final ImportacionServicio importacion;

    public EmpresaControlador(OfertaServicio ofertas, PostulacionServicio postulaciones, PerfilServicio perfiles,
            ImportacionServicio importacion) {
        this.ofertas = ofertas;
        this.postulaciones = postulaciones;
        this.perfiles = perfiles;
        this.importacion = importacion;
    }

    @PostMapping("/ofertas")
    @ResponseStatus(HttpStatus.CREATED)
    public Oferta publicar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OfertaPeticion p) {
        return ofertas.publicar(Actual.id(jwt), new OfertaServicio.DatosOferta(
                p.titulo(), p.descripcion(), p.especialidadRequerida(), p.pais(), p.salario(), p.moneda()));
    }

    @GetMapping("/ofertas")
    public List<Oferta> misOfertas(@AuthenticationPrincipal Jwt jwt) { return ofertas.listarDeEmpresa(Actual.id(jwt)); }

    @PostMapping("/ofertas/{id}/cerrar")
    public Oferta cerrar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return ofertas.cerrar(Actual.id(jwt), id); }

    @GetMapping("/ofertas/{id}/postulaciones")
    public List<Postulacion> postulaciones(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return postulaciones.listarPorOferta(Actual.id(jwt), id);
    }

    @PutMapping("/postulaciones/{id}/estado")
    public Postulacion cambiarEstado(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String clave, @Valid @RequestBody EstadoPeticion p) {
        return postulaciones.cambiarEstado(Actual.id(jwt), clave, id, p.estado());
    }

    @GetMapping("/perfiles")
    public List<Perfil> filtrarPerfiles(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "") String especialidad,
            @RequestParam(defaultValue = "0") int minAnios) {
        return perfiles.filtrar(Actual.id(jwt), especialidad, minAnios);
    }

    @PostMapping("/ofertas/importar")
    public ImportacionServicio.Reporte importar(@AuthenticationPrincipal Jwt jwt, @RequestParam("archivo") MultipartFile archivo,
            @RequestParam String formato) throws IOException {
        return importacion.importarOfertas(Actual.id(jwt), archivo.getBytes(), formato);
    }

    @GetMapping("/ofertas/exportar")
    public ResponseEntity<byte[]> exportar(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "json") String formato) {
        return Descargas.de(importacion.exportarOfertas(Actual.id(jwt), formato), formato, "ofertas");
    }
}
