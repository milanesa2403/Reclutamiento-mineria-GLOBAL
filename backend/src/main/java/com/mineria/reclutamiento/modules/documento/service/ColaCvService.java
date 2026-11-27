package com.mineria.reclutamiento.modules.documento.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mineria.reclutamiento.core.config.AwsProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sqs.SqsClient;

@Service
public class ColaCvService {

    private static final Logger log = LoggerFactory.getLogger(ColaCvService.class);

    private final SqsClient sqs;
    private final AwsProperties props;
    private final ObjectMapper mapper;

    public ColaCvService(SqsClient sqs, AwsProperties props, ObjectMapper mapper) {
        this.sqs = sqs;
        this.props = props;
        this.mapper = mapper;
    }

    public boolean enviar(String bucket, String clave, String documentoId) {
        if (props.colaCv() == null || props.colaCv().isBlank()) {
            log.warn("APP_SQS_CV_URL no está configurada: el CV {} no se envió a revisión", clave);
            return false;
        }
        Map<String, String> mensaje = new LinkedHashMap<>();
        mensaje.put("bucket", bucket);
        mensaje.put("clave", clave);
        mensaje.put("documentoId", documentoId);
        try {
            String cuerpo = mapper.writeValueAsString(mensaje);
            sqs.sendMessage(b -> b.queueUrl(props.colaCv()).messageBody(cuerpo));
            log.info("CV enviado a la cola de revisión: {}", clave);
            return true;
        } catch (JsonProcessingException | SdkException e) {
            log.error("No se pudo enviar el CV {} a la cola de revisión: {}", clave, e.getMessage());
            return false;
        }
    }
}
