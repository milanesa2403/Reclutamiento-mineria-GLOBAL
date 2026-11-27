package com.mineria.reclutamiento.modules.documento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import com.mineria.reclutamiento.modules.documento.entity.TipoDocumento;
import com.mineria.reclutamiento.modules.documento.repository.DocumentoRepository;
import com.mineria.reclutamiento.modules.documento.storage.AlmacenamientoArchivos;
import com.mineria.reclutamiento.core.config.AwsProperties;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class DocumentoServiceTest {

    static final byte[] PDF = "%PDF-1.7\n...".getBytes(StandardCharsets.US_ASCII);
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0};
    static final Set<String> PERMITIDOS = Set.of(DocumentoService.PDF, DocumentoService.JPEG, DocumentoService.PNG);

    AlmacenamientoArchivos almacenamiento;
    DocumentoService service;

    @BeforeEach
    void setUp() {
        almacenamiento = mock(AlmacenamientoArchivos.class);
        service = new DocumentoService(almacenamiento, mock(DocumentoRepository.class),
                new AwsProperties("us-east-2", "bucket-prueba", "", "cvs", 300));
    }

    @Test
    void aceptaPdfPngYJpg_detectandoElTipoPorElContenido() {
        assertThat(service.validar(archivo("a.pdf", "application/pdf", PDF), PERMITIDOS).tipoMime()).isEqualTo("application/pdf");
        assertThat(service.validar(archivo("a.png", "image/png", PNG), PERMITIDOS).tipoMime()).isEqualTo("image/png");
        assertThat(service.validar(archivo("a.jpg", "image/jpeg", JPG), PERMITIDOS).tipoMime()).isEqualTo("image/jpeg");
    }

    @Test
    void rechazaUnEjecutableDisfrazadoDePdf() {
        assertThatThrownBy(() -> service.validar(archivo("contrato.pdf", "application/pdf", EXE), PERMITIDOS))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Formato de archivo no permitido");
    }

    @Test
    void rechazaUnFormatoValidoPeroNoPermitidoParaEseDocumento() {
        assertThatThrownBy(() -> service.validar(archivo("foto.png", "image/png", PNG), Set.of(DocumentoService.PDF)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rechazaArchivoVacioYMayorA5Mb() {
        assertThatThrownBy(() -> service.validar(archivo("a.pdf", "application/pdf", new byte[0]), PERMITIDOS))
                .isInstanceOf(BadRequestException.class);
        byte[] grande = new byte[(int) DocumentoService.MAX_BYTES + 1];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);
        assertThatThrownBy(() -> service.validar(archivo("a.pdf", "application/pdf", grande), PERMITIDOS))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("5 MB");
        assertThatThrownBy(() -> service.validar(null, PERMITIDOS)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void elNombreOriginalNoPuedeTraerRutas() {
        assertThat(DocumentoService.nombreSeguro("../../etc/passwd")).isEqualTo("passwd");
        assertThat(DocumentoService.nombreSeguro("C:\\Users\\x\\poder.pdf")).isEqualTo("poder.pdf");
        assertThat(DocumentoService.nombreSeguro("a\u0000b.pdf")).isEqualTo("ab.pdf");
        assertThat(DocumentoService.nombreSeguro(null)).isEqualTo("archivo");
    }

    @Test
    void laClaveLaGeneraElBackend_conUuid_ySinElNombreDelCliente() {
        var validado = service.validar(archivo("../../malicioso.pdf", "application/pdf", PDF), PERMITIDOS);
        String clave = service.subirAlAlmacenamiento(42L, TipoDocumento.ACREDITACION_EMPRESA, validado);

        assertThat(clave).matches("cvs/acreditacion_empresa/42/[0-9a-f-]{36}\\.pdf");
        assertThat(clave).doesNotContain("malicioso").doesNotContain("..");
        verify(almacenamiento).subir(eq(clave), eq(PDF), eq("application/pdf"));
        assertThat(validado.hashSha256()).hasSize(64);
    }

    @Test
    void eliminarSilenciosoNoPropagaErrores() {
        doThrow(new IllegalStateException("caído")).when(almacenamiento).eliminar(anyString());
        service.borrarArchivo("cvs/acreditacion_empresa/1/x.pdf");
        assertThatThrownBy(() -> service.eliminarArchivo("cvs/acreditacion_empresa/1/x.pdf"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static MockMultipartFile archivo(String nombre, String tipo, byte[] contenido) {
        return new MockMultipartFile("archivo", nombre, tipo, contenido);
    }
}
