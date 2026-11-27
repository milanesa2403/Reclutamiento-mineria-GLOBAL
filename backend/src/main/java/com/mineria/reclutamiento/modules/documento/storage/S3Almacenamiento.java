package com.mineria.reclutamiento.modules.documento.storage;

import com.mineria.reclutamiento.core.config.AwsProperties;
import java.time.Duration;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Component
public class S3Almacenamiento implements AlmacenamientoArchivos {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final AwsProperties props;

    public S3Almacenamiento(S3Client s3, S3Presigner presigner, AwsProperties props) {
        this.s3 = s3;
        this.presigner = presigner;
        this.props = props;
    }

    @Override
    public void subir(String clave, byte[] contenido, String tipoMime) {
        s3.putObject(b -> b.bucket(bucket()).key(clave).contentType(tipoMime)
                .serverSideEncryption(ServerSideEncryption.AWS_KMS), RequestBody.fromBytes(contenido));
    }

    @Override
    public void eliminar(String clave) {
        s3.deleteObject(b -> b.bucket(bucket()).key(clave));
    }

    @Override
    public String urlFirmada(String clave, int segundos) {
        return presigner.presignGetObject(p -> p.signatureDuration(Duration.ofSeconds(segundos))
                .getObjectRequest(g -> g.bucket(bucket()).key(clave))).url().toString();
    }

    @Override
    public String bucket() {
        if (props.bucket() == null || props.bucket().isBlank()) {
            throw new AlmacenamientoNoConfiguradoException();
        }
        return props.bucket();
    }
}
