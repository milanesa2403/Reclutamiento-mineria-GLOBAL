package com.mineria.reclutamiento.modules.convocatoria.service;

import com.mineria.reclutamiento.modules.convocatoria.entity.Convocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.EstadoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.repository.ConvocatoriaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CierreConvocatoriasService {

    private static final Logger log = LoggerFactory.getLogger(CierreConvocatoriasService.class);

    private final ConvocatoriaRepository convocatoriaRepository;
    private final Clock clock;
    private final TransactionTemplate transaccion;
    private final boolean habilitado;

    public CierreConvocatoriasService(ConvocatoriaRepository convocatoriaRepository, Clock clock,
                                                PlatformTransactionManager transactionManager,
                                                @Value("${app.tareas-programadas:true}") boolean habilitado) {
        this.habilitado = habilitado;
        this.convocatoriaRepository = convocatoriaRepository;
        this.clock = clock;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        if (!habilitado) {
            return;
        }
        try {
            cerrarVencidas(LocalDate.now(clock));
        } catch (RuntimeException e) {
            log.warn("No se pudo ejecutar el cierre automático al arrancar (se reintentará a las 00:05): {}",
                    NestedExceptionUtils.getMostSpecificCause(e).toString());
        }
    }

    @Scheduled(cron = "${app.convocatorias.cierre-automatico-cron:0 5 0 * * *}", zone = "America/Lima")
    public void ejecutarProgramada() {
        if (habilitado) {
            cerrarVencidas(LocalDate.now(clock));
        }
    }

    public int cerrarVencidas(LocalDate hoy) {
        List<Convocatoria> vencidas = transaccion.execute(status -> {
            List<Convocatoria> lista = convocatoriaRepository
                    .findByEstadoAndFechaCierreBefore(EstadoConvocatoria.PUBLICADA, hoy);
            lista.forEach(Convocatoria::cerrar);
            return lista;
        });
        if (!vencidas.isEmpty()) {
            log.info("Cierre automático por fecha: {} convocatorias", vencidas.size());
        }
        return vencidas.size();
    }
}
