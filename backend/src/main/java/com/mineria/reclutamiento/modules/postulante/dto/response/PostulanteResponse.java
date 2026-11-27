package com.mineria.reclutamiento.modules.postulante.dto.response;

import com.mineria.reclutamiento.modules.persona.dto.response.PersonaResponse;

public record PostulanteResponse(
        Long idPostulante,
        Long idPersona,
        PersonaResponse persona,
        String resumenProfesional,
        String urlCv,
        String urlFotoDni,
        String urlCul
) {
}
