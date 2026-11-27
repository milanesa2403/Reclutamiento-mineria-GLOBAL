package com.mineria.reclutamiento.modules.documento.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mineria.reclutamiento.core.config.AwsProperties;
import com.mineria.reclutamiento.modules.documento.service.ColaCvService;
import java.net.URI;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

class S3AlmacenamientoTest {

    final S3Client s3 = mock(S3Client.class);
    final S3Presigner presigner = mock(S3Presigner.class);
    final AwsProperties props = new AwsProperties("us-east-2", "bucket-cvs", "https://sqs/cola-cv", null, 0);

    @Test
    @SuppressWarnings("unchecked")
    void subeConCifradoKms_yBorraDelMismoBucket() {
        var almacenamiento = new S3Almacenamiento(s3, presigner, props);

        almacenamiento.subir("cvs/cv/1/a.pdf", "%PDF-".getBytes(), "application/pdf");
        almacenamiento.eliminar("cvs/cv/1/a.pdf");

        ArgumentCaptor<Consumer<PutObjectRequest.Builder>> put = ArgumentCaptor.forClass(Consumer.class);
        verify(s3).putObject(put.capture(), any(RequestBody.class));
        PutObjectRequest.Builder b = PutObjectRequest.builder();
        put.getValue().accept(b);
        PutObjectRequest req = b.build();
        assertThat(req.bucket()).isEqualTo("bucket-cvs");
        assertThat(req.key()).isEqualTo("cvs/cv/1/a.pdf");
        assertThat(req.serverSideEncryption()).isEqualTo(ServerSideEncryption.AWS_KMS);

        ArgumentCaptor<Consumer<DeleteObjectRequest.Builder>> del = ArgumentCaptor.forClass(Consumer.class);
        verify(s3).deleteObject(del.capture());
        DeleteObjectRequest.Builder d = DeleteObjectRequest.builder();
        del.getValue().accept(d);
        assertThat(d.build().bucket()).isEqualTo("bucket-cvs");
    }

    @Test
    @SuppressWarnings("unchecked")
    void urlFirmada_usaElPresigner() throws Exception {
        PresignedGetObjectRequest firmada = mock(PresignedGetObjectRequest.class);
        when(firmada.url()).thenReturn(URI.create("https://bucket-cvs.s3.amazonaws.com/x?firma").toURL());
        when(presigner.presignGetObject(any(Consumer.class))).thenReturn(firmada);

        String url = new S3Almacenamiento(s3, presigner, props).urlFirmada("cvs/cv/1/a.pdf", 300);

        assertThat(url).startsWith("https://bucket-cvs.s3.amazonaws.com/");
        ArgumentCaptor<Consumer<GetObjectPresignRequest.Builder>> c = ArgumentCaptor.forClass(Consumer.class);
        verify(presigner).presignGetObject(c.capture());
        GetObjectPresignRequest.Builder b = GetObjectPresignRequest.builder();
        c.getValue().accept(b);
        assertThat(b.build().signatureDuration().getSeconds()).isEqualTo(300);
    }

    @Test
    void sinBucket_avisaQueNoHayAlmacenamiento() {
        var sinBucket = new S3Almacenamiento(s3, presigner, new AwsProperties(null, "", null, null, 0));
        assertThatThrownBy(sinBucket::bucket).isInstanceOf(AlmacenamientoNoConfiguradoException.class);
    }

    @Test
    void valoresPorDefecto() {
        var p = new AwsProperties(null, null, null, null, 0);
        assertThat(p.region()).isEqualTo("us-east-2");
        assertThat(p.prefijo()).isEqualTo("cvs");
        assertThat(p.urlFirmadaSegundos()).isEqualTo(300);
    }

    @Test
    @SuppressWarnings("unchecked")
    void colaCv_enviaBucketClaveYDocumento() {
        SqsClient sqs = mock(SqsClient.class);
        var cola = new ColaCvService(sqs, props, new ObjectMapper());

        assertThat(cola.enviar("bucket-cvs", "cvs/cv/1/a.pdf", "postulante-3")).isTrue();

        ArgumentCaptor<Consumer<SendMessageRequest.Builder>> c = ArgumentCaptor.forClass(Consumer.class);
        verify(sqs).sendMessage(c.capture());
        SendMessageRequest.Builder b = SendMessageRequest.builder();
        c.getValue().accept(b);
        assertThat(b.build().queueUrl()).isEqualTo("https://sqs/cola-cv");
        assertThat(b.build().messageBody())
                .isEqualTo("{\"bucket\":\"bucket-cvs\",\"clave\":\"cvs/cv/1/a.pdf\",\"documentoId\":\"postulante-3\"}");
    }

    @Test
    @SuppressWarnings("unchecked")
    void colaCv_sinUrl_oConError_noRompeLaSubida() {
        SqsClient sqs = mock(SqsClient.class);
        assertThat(new ColaCvService(sqs, new AwsProperties(null, "b", "", null, 0), new ObjectMapper())
                .enviar("b", "c", "d")).isFalse();

        when(sqs.sendMessage(any(Consumer.class))).thenThrow(SqsException.builder().message("caída").build());
        assertThat(new ColaCvService(sqs, props, new ObjectMapper()).enviar("b", "c", "d")).isFalse();
    }
}
