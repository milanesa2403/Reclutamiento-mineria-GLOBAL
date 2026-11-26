package com.mineria.reclutamiento.modules.postulante.controller;

import com.mineria.reclutamiento.core.response.ApiResponse;
import com.mineria.reclutamiento.modules.documento.dto.UrlTemporalResponse;
import com.mineria.reclutamiento.modules.postulante.dto.request.ActualizarPostulanteRequest;
import com.mineria.reclutamiento.modules.postulante.dto.request.PerfilProfesionalRequest;
import com.mineria.reclutamiento.modules.postulante.dto.response.PerfilPostulanteResponse;
import com.mineria.reclutamiento.modules.postulante.dto.response.PerfilProfesionalResponse;
import com.mineria.reclutamiento.modules.postulante.dto.response.PostulanteResponse;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.postulante.dto.response.DocumentosPostulanteResponse;
import com.mineria.reclutamiento.modules.postulante.entity.DocumentoPostulante;
import com.mineria.reclutamiento.modules.documento.dto.DocumentoResponse;
import com.mineria.reclutamiento.modules.postulante.service.CertificadoArchivoService;
import com.mineria.reclutamiento.modules.postulante.service.DocumentosPostulanteService;
import com.mineria.reclutamiento.modules.postulante.service.PerfilProfesionalService;
import com.mineria.reclutamiento.modules.postulante.service.PostulanteService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/postulantes")
public class PostulanteController {

    private final PostulanteService postulanteService;
    private final PerfilProfesionalService perfilProfesionalService;
    private final DocumentosPostulanteService documentosService;
    private final CertificadoArchivoService certificadoArchivoService;

    public PostulanteController(PostulanteService postulanteService,
                                PerfilProfesionalService perfilProfesionalService,
                                DocumentosPostulanteService documentosService,
                                CertificadoArchivoService certificadoArchivoService) {
        this.postulanteService = postulanteService;
        this.perfilProfesionalService = perfilProfesionalService;
        this.documentosService = documentosService;
        this.certificadoArchivoService = certificadoArchivoService;
    }


    @PreAuthorize("hasRole('POSTULANTE')")
    @GetMapping("/me")
    public ApiResponse<PerfilPostulanteResponse> miPerfil(Authentication authentication) {
        return ApiResponse.ok(postulanteService.obtenerMiPerfil(authentication.getName()));
    }

    @PreAuthorize("hasRole('POSTULANTE')")
    @PutMapping("/me")
    public ApiResponse<PerfilPostulanteResponse> actualizarMiPerfil(
            Authentication authentication,
            @Valid @RequestBody ActualizarPostulanteRequest request) {
        return ApiResponse.ok("Perfil actualizado correctamente",
                postulanteService.actualizarMiPerfil(authentication.getName(), request));
    }


    @PreAuthorize("hasRole('POSTULANTE')")
    @PostMapping(path = "/me/documentos/{tipo}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentosPostulanteResponse> subirMiDocumento(Authentication authentication,
                                                                      @PathVariable String tipo,
                                                                      @RequestParam("archivo") MultipartFile archivo) {
        return ApiResponse.ok("Documento actualizado correctamente",
                documentosService.subir(authentication.getName(), documento(tipo), archivo));
    }

    @PreAuthorize("hasRole('POSTULANTE')")
    @GetMapping("/me/documentos/{tipo}/url")
    public ApiResponse<UrlTemporalResponse> urlMiDocumento(Authentication authentication, @PathVariable String tipo) {
        return ApiResponse.ok(documentosService.url(authentication.getName(), documento(tipo)));
    }


    @PreAuthorize("hasRole('POSTULANTE')")
    @PostMapping(path = "/me/certificaciones/{id}/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentoResponse> subirPdfCertificacion(Authentication authentication, @PathVariable Long id,
                                                                @RequestParam("archivo") MultipartFile archivo) {
        return ApiResponse.ok("PDF de la certificación actualizado",
                certificadoArchivoService.subir(authentication.getName(), id, archivo));
    }

    @PreAuthorize("hasRole('POSTULANTE')")
    @GetMapping("/me/certificaciones/{id}/archivo/url")
    public ApiResponse<UrlTemporalResponse> urlPdfCertificacion(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.ok(certificadoArchivoService.url(authentication.getName(), id));
    }

    private static DocumentoPostulante documento(String tipo) {
        return DocumentoPostulante.desdeRuta(tipo)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de documento no válido: usa cv, foto-dni, cul o emo"));
    }


    @PreAuthorize("hasRole('POSTULANTE')")
    @GetMapping("/me/perfil")
    public ApiResponse<PerfilProfesionalResponse> miPerfilProfesional(Authentication authentication) {
        return ApiResponse.ok(perfilProfesionalService.obtener(authentication.getName()));
    }

    @PreAuthorize("hasRole('POSTULANTE')")
    @PutMapping("/me/perfil")
    public ApiResponse<PerfilProfesionalResponse> guardarPerfil(
            Authentication authentication,
            @Valid @RequestBody PerfilProfesionalRequest request) {
        return ApiResponse.ok("Perfil profesional creado correctamente",
                perfilProfesionalService.reemplazar(authentication.getName(), request));
    }


    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/{id}")
    public ResponseEntity<PostulanteResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(postulanteService.obtenerPorId(id));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/persona/{idPersona}")
    public ResponseEntity<PostulanteResponse> obtenerPorPersonaId(@PathVariable Long idPersona) {
        return ResponseEntity.ok(postulanteService.obtenerPorPersonaId(idPersona));
    }
}
