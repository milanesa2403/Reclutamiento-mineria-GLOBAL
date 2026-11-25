package com.mineria.reclutamiento.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RNF 29: documentacion OpenAPI 3.0 en /v3/api-docs y /swagger-ui.html. */
@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("API Reclutamiento Minero Global").version("v1")
                        .description("Plataforma global para reclutamiento de mineria. Version /v1 (compatibilidad minima 6 meses)."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
