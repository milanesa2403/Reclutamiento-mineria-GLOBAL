package com.mineria.reclutamiento.modules.postulante.dto.response;

public record DocumentosPostulanteResponse(Documento cv, Documento fotoDni, Documento cul, Documento emo) {

    public record Documento(String formato) {
    }
}
