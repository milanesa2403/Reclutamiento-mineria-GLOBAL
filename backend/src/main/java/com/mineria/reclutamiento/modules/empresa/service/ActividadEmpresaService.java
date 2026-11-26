package com.mineria.reclutamiento.modules.empresa.service;

import com.mineria.reclutamiento.modules.empresa.entity.ActividadEmpresa;
import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import com.mineria.reclutamiento.modules.empresa.entity.TipoActividadEmpresa;
import com.mineria.reclutamiento.modules.empresa.repository.ActividadEmpresaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActividadEmpresaService {

    private final ActividadEmpresaRepository repository;

    public ActividadEmpresaService(ActividadEmpresaRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Empresa empresa, TipoActividadEmpresa tipo, String motivo, String realizadoPor) {
        repository.save(new ActividadEmpresa(
                empresa.getId(), empresa.getRuc(), empresa.getRazonSocial(), tipo, motivo, realizadoPor));
    }

    @Transactional(readOnly = true)
    public List<ActividadEmpresa> historial(Long idEmpresa) {
        return repository.findByIdEmpresaOrderByFechaDescIdDesc(idEmpresa);
    }

    @Transactional(readOnly = true)
    public long contarAdvertencias(Long idEmpresa) {
        return repository.countByIdEmpresaAndTipo(idEmpresa, TipoActividadEmpresa.ADVERTENCIA);
    }
}