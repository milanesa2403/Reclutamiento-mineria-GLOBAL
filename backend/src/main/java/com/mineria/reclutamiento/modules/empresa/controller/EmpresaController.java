package com.mineria.reclutamiento.modules.empresa.controller;

import com.mineria.reclutamiento.modules.empresa.dto.request.EmpresaPerfilUpdateRequest;
import com.mineria.reclutamiento.modules.empresa.dto.response.EmpresaPerfilResponse;
import com.mineria.reclutamiento.modules.empresa.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empresa")
@PreAuthorize("hasRole('EMPRESA')")
public class EmpresaController {

    private final EmpresaService empresaService;
    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping("/perfil")
    public ResponseEntity<EmpresaPerfilResponse> obtenerPerfil(
            Authentication authentication) {

        String correo = authentication.getName();

        EmpresaPerfilResponse perfil =
                empresaService.perfilDe(correo);

        return ResponseEntity.ok(perfil);
    }

    @PutMapping("/perfil")
    public ResponseEntity<EmpresaPerfilResponse> actualizarPerfil(
            Authentication authentication,
            @Valid @RequestBody EmpresaPerfilUpdateRequest request) {

        String correo = authentication.getName();

        EmpresaPerfilResponse perfil =
                empresaService.actualizarPerfil(correo, request);

        return ResponseEntity.ok(perfil);
    }

}
