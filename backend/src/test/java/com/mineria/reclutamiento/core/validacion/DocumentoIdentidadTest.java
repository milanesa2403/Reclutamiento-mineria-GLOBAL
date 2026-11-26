package com.mineria.reclutamiento.core.validacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.mineria.reclutamiento.modules.persona.entity.TipoDocumentoIdentidad;
import org.junit.jupiter.api.Test;

class DocumentoIdentidadTest {

    @Test
    void normalizaMayusculasYEspacios() {
        assertThat(DocumentoIdentidad.normalizar(" ab 12-c ")).isEqualTo("AB12-C");
        assertThat(DocumentoIdentidad.normalizar(null)).isEmpty();
    }

    @Test
    void paisDebeSerCodigoDeDosLetras() {
        assertThat(DocumentoIdentidad.problemaPais("cl")).isNull();
        assertThat(DocumentoIdentidad.problemaPais("CHL")).isNotNull();
        assertThat(DocumentoIdentidad.problemaPais("")).isNotNull();
    }

    @Test
    void dniPeruanoExigeOchoDigitos() {
        assertThat(DocumentoIdentidad.problemaDocumento("PE", TipoDocumentoIdentidad.DNI, "12345678")).isNull();
        assertThat(DocumentoIdentidad.problemaDocumento("PE", TipoDocumentoIdentidad.DNI, "1234567A")).isNotNull();
    }

    @Test
    void otrosDocumentosUsanLaReglaGeneral() {
        assertThat(DocumentoIdentidad.problemaDocumento("PE", TipoDocumentoIdentidad.CARNE_EXTRANJERIA, "001234567")).isNull();
        assertThat(DocumentoIdentidad.problemaDocumento("AR", TipoDocumentoIdentidad.DNI, "30123456")).isNull();
        assertThat(DocumentoIdentidad.problemaDocumento("CA", TipoDocumentoIdentidad.PASAPORTE, "AB1")).isNotNull();
        assertThat(DocumentoIdentidad.problemaDocumento("CA", null, "AB123456")).isNotNull();
    }

    @Test
    void rucDeEmpresaSoloSeExigeEnPeru() {
        assertThat(DocumentoIdentidad.problemaIdentificadorFiscal("PE", "20123456789")).isNull();
        assertThat(DocumentoIdentidad.problemaIdentificadorFiscal("PE", "10123456789")).isNotNull();
        assertThat(DocumentoIdentidad.problemaIdentificadorFiscal("CL", "76123456-7")).isNull();
        assertThat(DocumentoIdentidad.problemaIdentificadorFiscal("AU", "51 824 753 556")).isNull();
        assertThat(DocumentoIdentidad.problemaIdentificadorFiscal("AU", "51/824")).isNotNull();
    }
}
