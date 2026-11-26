package com.mineria.reclutamiento.modules.admin.dto;

import com.mineria.reclutamiento.modules.documento.dto.DocumentoResponse;
import com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa;
import com.mineria.reclutamiento.modules.empresa.entity.TipoActividadEmpresa;
import java.time.LocalDateTime;
import java.util.List;

public record EmpresaAdminDetalleResponse(
        Long idEmpresa,
        String pais,
        String ruc,
        String razonSocial,
        String direccion,
        String telefono,
        String correoCorporativo,
        String sectorMinero,
        String descripcion,
        LocalDateTime fechaRegistro,
        EstadoEmpresa estado,
        String motivoRevision,
        LocalDateTime fechaRevision,
        String revisadoPor,
        DocumentoResponse documentoAcreditacion,
        List<Solicitante> solicitantes,
        List<RechazoAnterior> rechazosAnteriores,
        long advertencias,
        List<Evento> historial
) {

    public record Solicitante(String pais, String tipoDocumento, String dni, String nombres, String apellidos, String cargo, String correo,
                              String telefono) {
    }

    public record RechazoAnterior(String motivo, LocalDateTime fechaRechazo) {
    }

    public record Evento(TipoActividadEmpresa tipo, String motivo, String realizadoPor, LocalDateTime fecha) {
    }
}
