package com.mineria.reclutamiento.core.config;

import com.mineria.reclutamiento.core.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/configuracion")
public class ConfiguracionPublicaController {

    private final CognitoProperties cognito;

    public ConfiguracionPublicaController(CognitoProperties cognito) {
        this.cognito = cognito;
    }

    public record ConfiguracionResponse(String region, String userPoolId, String clientId) {
    }

    @GetMapping
    public ApiResponse<ConfiguracionResponse> obtener() {
        return ApiResponse.ok("Configuración", new ConfiguracionResponse(cognito.region(), cognito.userPoolId(),
                cognito.clientId() == null ? "" : cognito.clientId()));
    }
}
