package com.mineria.reclutamiento.modules.admin.controller;

import com.mineria.reclutamiento.core.response.ApiResponse;
import com.mineria.reclutamiento.modules.admin.dto.EmpresaAdminDetalleResponse;
import com.mineria.reclutamiento.modules.admin.dto.EmpresaAdminItemResponse;
import com.mineria.reclutamiento.modules.admin.dto.MotivoDecisionRequest;
import com.mineria.reclutamiento.modules.admin.service.AdminEmpresaService;
import com.mineria.reclutamiento.modules.documento.dto.UrlTemporalResponse;
import com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/empresas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminEmpresaController {

    private final AdminEmpresaService service;

    public AdminEmpresaController(AdminEmpresaService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<EmpresaAdminItemResponse>> listar(
            @RequestParam(name = "estado", required = false) EstadoEmpresa estado) {
        return ApiResponse.ok(service.listar(estado));
    }

    @GetMapping("/{id}")
    public ApiResponse<EmpresaAdminDetalleResponse> detalle(@PathVariable Long id) {
        return ApiResponse.ok(service.detalle(id));
    }

    @GetMapping("/{id}/documento/url")
    public ApiResponse<UrlTemporalResponse> urlDocumento(@PathVariable Long id) {
        return ApiResponse.ok(service.urlDocumento(id));
    }

    @PostMapping("/{id}/aprobar")
    public ApiResponse<EmpresaAdminDetalleResponse> aprobar(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("Empresa aprobada.", service.aprobar(id, authentication.getName()));
    }

    @PostMapping("/{id}/observar")
    public ApiResponse<EmpresaAdminDetalleResponse> observar(@PathVariable Long id,
                                                             @Valid @RequestBody MotivoDecisionRequest request,
                                                             Authentication authentication) {
        return ApiResponse.ok("Solicitud observada.",
                service.observar(id, request.motivo(), authentication.getName()));
    }

    @PostMapping("/{id}/rechazar")
    public ApiResponse<EmpresaAdminDetalleResponse> rechazar(@PathVariable Long id,
                                                             @Valid @RequestBody MotivoDecisionRequest request,
                                                             Authentication authentication) {
        return ApiResponse.ok("Solicitud rechazada.",
                service.rechazar(id, request.motivo(), authentication.getName()));
    }
}
