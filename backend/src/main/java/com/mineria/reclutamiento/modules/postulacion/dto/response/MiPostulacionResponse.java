package com.mineria.reclutamiento.modules.postulacion.dto.response;

import com.mineria.reclutamiento.modules.convocatoria.entity.EstadoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.TipoConvocatoria;
import com.mineria.reclutamiento.modules.postulacion.entity.Entrevista;
import com.mineria.reclutamiento.modules.postulacion.entity.EstadoPostulacion;
import com.mineria.reclutamiento.modules.ubigeo.dto.UbicacionResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MiPostulacionResponse(
        Long idPostulacion,
        Long idConvocatoria,
        String puesto,
        String empresa,
        TipoConvocatoria tipo,
        String campamento,
        EstadoConvocatoria estadoConvocatoria,
        EstadoPostulacion estado,
        LocalDateTime fechaPostulacion,
        LocalDateTime fechaCambioEstado,
        List<String> certificacionesAdjuntas,
        UbicacionResponse ubicacion,
        LocalDate fechaCierre,
        LocalDateTime fechaLimiteConfirmacion,
        String motivoRetiro,
        boolean visto,
        Entrevista entrevista,
        String logoUrl,
        String turno,
        LocalDate fechaInicioLabores,
        LocalDate fechaFinLabores,
        String direccion,
        String referencia,
        String descripcion
) {
}
