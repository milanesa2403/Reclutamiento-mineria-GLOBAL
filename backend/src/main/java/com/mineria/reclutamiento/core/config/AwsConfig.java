package com.mineria.reclutamiento.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfig {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(AwsProperties props) {
        return S3Client.builder().region(Region.of(props.region())).build();
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(AwsProperties props) {
        return S3Presigner.builder().region(Region.of(props.region())).build();
    }

    @Bean(destroyMethod = "close")
    public SqsClient sqsClient(AwsProperties props) {
        return SqsClient.builder().region(Region.of(props.region())).build();
    }
}
