package com.mineria.reclutamiento.servicio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cola en memoria que imita a SQS + DLQ (RNF 5 y 24): en modo solo lectura las postulaciones se encolan,
 * se procesan cuando vuelve la escritura, se reintentan hasta 5 veces y luego pasan a la DLQ.
 * En AWS se reemplaza por SQS (misma interfaz de uso).
 */
@Component
public class ColaPostulaciones {
    public static final int MAX_INTENTOS = 5;
    private static final Logger log = LoggerFactory.getLogger(ColaPostulaciones.class);

    public record Pendiente(String clave, UUID ofertaId, UUID trabajadorId, UUID cvId, int intentos, String ultimoError) {
        Pendiente conIntento(String error) { return new Pendiente(clave, ofertaId, trabajadorId, cvId, intentos + 1, error); }
    }

    private final ConcurrentLinkedQueue<Pendiente> cola = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Pendiente> dlq = new ConcurrentLinkedQueue<>();
    private final PostulacionServicio postulaciones;
    private final EstadoSistema estado;

    public ColaPostulaciones(PostulacionServicio postulaciones, EstadoSistema estado) {
        this.postulaciones = postulaciones;
        this.estado = estado;
    }

    public void encolar(String clave, UUID ofertaId, UUID trabajadorId, UUID cvId) {
        cola.add(new Pendiente(clave, ofertaId, trabajadorId, cvId, 0, null));
    }

    @Scheduled(fixedDelayString = "${app.cola.intervalo-ms:5000}")
    public void procesar() {
        if (estado.isSoloLectura()) return;
        Pendiente p;
        int limite = cola.size();
        while (limite-- > 0 && (p = cola.poll()) != null) {
            try {
                postulaciones.postular(p.clave(), p.ofertaId(), p.trabajadorId(), p.cvId());
            } catch (Exception e) {
                Pendiente intento = p.conIntento(e.getMessage());
                if (intento.intentos() >= MAX_INTENTOS) {
                    dlq.add(intento);
                    log.error("ALERTA_DLQ postulacion movida a DLQ tras {} intentos: {}", MAX_INTENTOS, e.getMessage());
                } else {
                    cola.add(intento);
                }
            }
        }
    }

    /** Devuelve los mensajes de la DLQ a la cola sin que el usuario tenga que postular de nuevo. */
    public int reprocesarDlq() {
        int n = 0;
        Pendiente p;
        while ((p = dlq.poll()) != null) {
            cola.add(new Pendiente(p.clave(), p.ofertaId(), p.trabajadorId(), p.cvId(), 0, null));
            n++;
        }
        return n;
    }

    public Map<String, Object> resumen() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("soloLectura", estado.isSoloLectura());
        m.put("pendientes", cola.size());
        m.put("dlq", dlq.size());
        List<String> errores = dlq.stream().map(Pendiente::ultimoError).toList();
        m.put("erroresDlq", errores);
        return m;
    }
}
