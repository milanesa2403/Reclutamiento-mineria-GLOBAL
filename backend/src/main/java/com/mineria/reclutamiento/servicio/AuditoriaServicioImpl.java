package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.modelo.RegistroAuditoria;
import com.mineria.reclutamiento.repositorio.AuditoriaRepositorio;
import com.mineria.reclutamiento.util.IpCliente;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaServicioImpl implements AuditoriaServicio {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaServicioImpl.class);
    private final AuditoriaRepositorio repo;
    private final BloqueoIpServicio bloqueo;
    private final int maxFallos;
    private final int ventanaMin;
    private final int bloqueoMin;

    public AuditoriaServicioImpl(AuditoriaRepositorio repo, BloqueoIpServicio bloqueo,
            @Value("${seguridad.ip.max-fallos:10}") int maxFallos,
            @Value("${seguridad.ip.ventana-minutos:10}") int ventanaMin,
            @Value("${seguridad.ip.bloqueo-minutos:15}") int bloqueoMin) {
        this.repo = repo;
        this.bloqueo = bloqueo;
        this.maxFallos = maxFallos;
        this.ventanaMin = ventanaMin;
        this.bloqueoMin = bloqueoMin;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(UUID usuarioId, String accion, String recurso, boolean permitido) {
        String ip = IpCliente.actual();
        repo.save(new RegistroAuditoria(usuarioId, accion, recurso, permitido, ip));
        if (!permitido && ip != null) {
            long fallos = repo.countByIpAndPermitidoFalseAndFechaAfter(ip, Instant.now().minus(Duration.ofMinutes(ventanaMin)));
            if (fallos > maxFallos && bloqueo.segundosRestantes(ip) == 0) {
                bloqueo.bloquear(ip, Duration.ofMinutes(bloqueoMin));
                // En AWS esto se envia a SNS; aqui queda en el log estructurado (metrica de CloudWatch).
                log.warn("ALERTA_SEGURIDAD ip={} bloqueada {} min por {} intentos fallidos/denegados", ip, bloqueoMin, fallos);
            }
        }
    }

    @Override
    public List<RegistroAuditoria> recientes() { return repo.findTop100ByOrderByFechaDesc(); }
}
