package com.mineria.reclutamiento.modules.empresa.dto.response;


public record EmpresaPerfilResponse(
        Long idEmpresa,
        String pais,
        String ruc,
        String razonSocial,
        String direccion,
        String telefono,
        String correoCorporativo,
        String sectorMinero,
        String tipoDocumentoRepresentante,
        String dniRepresentante,
        String nombresRepresentante,
        String apellidosRepresentante,
        String telefonoRepresentante,
        String direccionRepresentante,
        String cargoRepresentante,
        Long idSectorMinero,
        String descripcion,
        String imagenUrl,
        String bannerUrl,
        String estado,
        String motivoRevision
) {
}
