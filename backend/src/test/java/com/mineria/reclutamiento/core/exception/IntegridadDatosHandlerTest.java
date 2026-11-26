package com.mineria.reclutamiento.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class IntegridadDatosHandlerTest {

    final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void duplicado_23505_es409() {
        var ex = new DataIntegrityViolationException("x",
                new SQLException("duplicate key value violates unique constraint \"usuario_correo_key\"", "23505"));
        var res = handler.integridad(ex);
        assertThat(res.getStatusCode().value()).isEqualTo(409);
        assertThat(res.getBody().message()).contains("ya está registrado").doesNotContain("usuario_correo_key");
    }

    @Test
    void otraRestriccion_tambienEs409_sinDetallesInternos() {
        var ex = new DataIntegrityViolationException("x", new SQLException("violates foreign key \"fk_x\"", "23503"));
        var res = handler.integridad(ex);
        assertThat(res.getStatusCode().value()).isEqualTo(409);
        assertThat(res.getBody().message()).doesNotContain("fk_x");
    }
}
