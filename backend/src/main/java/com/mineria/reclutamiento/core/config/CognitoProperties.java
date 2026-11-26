package com.mineria.reclutamiento.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cognito")
public record CognitoProperties(String issuerUri, String clientId) {

    public String userPoolId() {
        if (issuerUri == null || issuerUri.isBlank()) {
            return "";
        }
        return issuerUri.substring(issuerUri.lastIndexOf('/') + 1);
    }

    public String region() {
        String pool = userPoolId();
        int guion = pool.indexOf('_');
        return guion > 0 ? pool.substring(0, guion) : "";
    }
}
