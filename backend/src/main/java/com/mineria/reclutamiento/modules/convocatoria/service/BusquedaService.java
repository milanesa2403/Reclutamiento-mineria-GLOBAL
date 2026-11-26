package com.mineria.reclutamiento.modules.convocatoria.service;

import com.mineria.reclutamiento.core.exception.DuplicateResourceException;
import com.mineria.reclutamiento.core.exception.ResourceNotFoundException;
import com.mineria.reclutamiento.modules.convocatoria.dto.BusquedaResponse;
import com.mineria.reclutamiento.modules.convocatoria.entity.Busqueda;
import com.mineria.reclutamiento.modules.convocatoria.repository.BusquedaRepository;
import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import com.mineria.reclutamiento.modules.empresa.service.EmpresaActual;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusquedaService {

    private final EmpresaActual empresaActual;
    private final BusquedaRepository repository;

    public BusquedaService(EmpresaActual empresaActual, BusquedaRepository repository) {
        this.empresaActual = empresaActual;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BusquedaResponse> listar(String correo) {
        Long idEmpresa = empresaActual.obtenerAprobada(correo).empresa().getId();
        return repository.deEmpresa(idEmpresa).stream()
                .map(BusquedaResponse::de)
                .toList();
    }

    @Transactional
    public BusquedaResponse crear(String correo, String nombre) {
        Empresa empresa = empresaActual.obtenerAprobada(correo).empresa();
        String limpio = nombre.trim();
        if (repository.nombreUsado(empresa.getId(), limpio)) {
            throw new DuplicateResourceException("Ya tienes una búsqueda con ese nombre.");
        }
        return BusquedaResponse.de(repository.saveAndFlush(new Busqueda(empresa, limpio)));
    }

    @Transactional
    public BusquedaResponse renombrar(String correo, Long id, String nombre) {
        Busqueda b = buscarPropia(correo, id);
        String limpio = nombre.trim();
        if (repository.nombreUsadoEnOtra(b.getEmpresa().getId(), limpio, id)) {
            throw new DuplicateResourceException("Ya tienes una búsqueda con ese nombre.");
        }
        b.setNombre(limpio);
        return BusquedaResponse.de(b);
    }

    @Transactional
    public void eliminar(String correo, Long id) {
        repository.delete(buscarPropia(correo, id));
    }

    private Busqueda buscarPropia(String correo, Long id) {
        Long idEmpresa = empresaActual.obtenerAprobada(correo).empresa().getId();
        return repository.findById(id)
                .filter(b -> b.getEmpresa().getId().equals(idEmpresa))
                .orElseThrow(() -> new ResourceNotFoundException("Búsqueda no encontrada"));
    }
}
