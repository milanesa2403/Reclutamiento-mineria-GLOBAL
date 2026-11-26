package com.mineria.reclutamiento.modules.postulacion.entity;

import com.mineria.reclutamiento.core.exception.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

@Embeddable
public record Entrevista(
        @Column(name = "entrevista_fecha") LocalDateTime fecha,
        @Column(name = "entrevista_modalidad", length = 10) String modalidad,
        @Column(name = "entrevista_lugar") String lugar,
        @Column(name = "entrevista_mensaje", length = 500) String mensaje
) {

    void validar(LocalDateTime ahora) {
        if (fecha == null || !fecha.isAfter(ahora)) {
            throw new BadRequestException("La fecha y la hora de la entrevista deben ser posteriores al momento actual.");
        }
        if (!"PRESENCIAL".equals(modalidad) && !"VIRTUAL".equals(modalidad)) {
            throw new BadRequestException("Elige la modalidad: Presencial o Virtual.");
        }
        if (lugar == null || lugar.isBlank() || lugar.length() > 255) {
            throw new BadRequestException("Indica el lugar o el enlace de la entrevista (máximo 255 caracteres).");
        }
        if (mensaje != null && mensaje.length() > 500) {
            throw new BadRequestException("El mensaje no puede superar los 500 caracteres.");
        }
    }
}
