package com.mineria.reclutamiento.modules.empresa.controller;

import com.mineria.reclutamiento.core.response.ApiResponse;
import com.mineria.reclutamiento.modules.documento.dto.UrlTemporalResponse;
import com.mineria.reclutamiento.modules.empresa.dto.response.SolicitudEmpresaResponse;
import com.mineria.reclutamiento.modules.empresa.service.SolicitudEmpresaService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/empresa/solicitud")
@PreAuthorize("hasRole('EMPRESA')")
public class SolicitudEmpresaController {

    private final SolicitudEmpresaService service;

    public SolicitudEmpresaController(SolicitudEmpresaService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<SolicitudEmpresaResponse> miSolicitud(Authentication authentication) {
        return ApiResponse.ok(service.obtenerMiSolicitud(authentication.getName()));
    }

    @PostMapping(path = "/documento", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SolicitudEmpresaResponse> subirDocumento(Authentication authentication,
                                                                @RequestParam("archivo") MultipartFile archivo) {
        return ApiResponse.ok("Documento recibido. Tu solicitud quedó en revisión.",
                service.subirAcreditacion(authentication.getName(), archivo));
    }

    @GetMapping("/documento/url")
    public ApiResponse<UrlTemporalResponse> urlDocumento(Authentication authentication) {
        return ApiResponse.ok(service.urlMiDocumento(authentication.getName()));
    }
}
