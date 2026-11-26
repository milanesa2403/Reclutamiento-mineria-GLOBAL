package com.mineria.reclutamiento.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.aws")
public record AwsProperties(String region, String bucket, String colaCv, String prefijo, int urlFirmadaSegundos) {

    public AwsProperties {
        region = region == null || region.isBlank() ? "us-east-2" : region;
        prefijo = prefijo == null || prefijo.isBlank() ? "cvs" : prefijo;
        urlFirmadaSegundos = urlFirmadaSegundos <= 0 ? 300 : urlFirmadaSegundos;
    }
}
