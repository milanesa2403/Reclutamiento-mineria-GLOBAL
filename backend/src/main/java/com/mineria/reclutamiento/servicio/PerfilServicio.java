package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.modelo.Perfil;
import java.util.List;
import java.util.UUID;

public interface PerfilServicio {
    record DatosPerfil(String nombreCompleto, String pais, String idioma, String especialidad, int aniosExperiencia) {}

    Perfil obtener(UUID usuarioId);
    Perfil actualizar(UUID usuarioId, DatosPerfil datos);
    /** Consulta de datos personales por empresa: queda en auditoria. */
    List<Perfil> filtrar(UUID solicitanteId, String especialidad, int minAniosExperiencia);
}
