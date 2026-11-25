package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.modelo.Empresa;
import com.mineria.reclutamiento.modelo.Oferta;
import java.util.List;
import java.util.UUID;

public interface OfertaServicio {
    record DatosOferta(String titulo, String descripcion, String especialidadRequerida,
                       String pais, double salario, String moneda) {}

    Oferta publicar(UUID usuarioEmpresaId, DatosOferta datos);
    Oferta obtener(UUID ofertaId);
    List<Oferta> buscar(String especialidad, String pais);
    List<Oferta> listarDeEmpresa(UUID usuarioEmpresaId);
    Oferta cerrar(UUID usuarioEmpresaId, UUID ofertaId);
    int archivarAntiguas(); // ofertas cerradas hace mas de 6 meses
    Empresa empresaDe(UUID usuarioId);
}
