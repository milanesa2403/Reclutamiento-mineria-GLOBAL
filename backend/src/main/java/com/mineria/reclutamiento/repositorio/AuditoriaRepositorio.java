package com.mineria.reclutamiento.repositorio;

import com.mineria.reclutamiento.modelo.RegistroAuditoria;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepositorio extends JpaRepository<RegistroAuditoria, Long> {
    long countByIpAndPermitidoFalseAndFechaAfter(String ip, Instant desde);
    List<RegistroAuditoria> findTop100ByOrderByFechaDesc();
    long deleteByFechaBefore(Instant limite);
}
