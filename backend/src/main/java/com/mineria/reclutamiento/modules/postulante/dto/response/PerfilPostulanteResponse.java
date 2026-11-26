package com.mineria.reclutamiento.modules.postulante.dto.response;


import java.util.List;

public record PerfilPostulanteResponse(
        String pais,
        String tipoDocumento,
        String dni,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        String direccion,
        String resumenProfesional,
        DocumentosPostulanteResponse documentos,
        boolean perfilCompleto,
        List<String> pendientes
) {
}
