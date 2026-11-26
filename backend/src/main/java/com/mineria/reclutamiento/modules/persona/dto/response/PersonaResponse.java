package com.mineria.reclutamiento.modules.persona.dto.response;


public record PersonaResponse(
        Long idPersona,
        Long idUsuario,
        String pais,
        String tipoDocumento,
        String dni,
        String nombres,
        String apellidos,
        String telefono,
        String direccion
) {
}
