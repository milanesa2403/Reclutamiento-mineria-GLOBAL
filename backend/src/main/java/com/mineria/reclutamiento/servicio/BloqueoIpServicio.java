package com.mineria.reclutamiento.servicio;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** RNF 11: IPs bloqueadas temporalmente (respuesta 429). */
@Component
public class BloqueoIpServicio {
    private final Map<String, Instant> bloqueos = new ConcurrentHashMap<>();

    public void bloquear(String ip, Duration duracion) { bloqueos.put(ip, Instant.now().plus(duracion)); }

    public long segundosRestantes(String ip) {
        Instant hasta = bloqueos.get(ip);
        if (hasta == null) return 0;
        long s = Duration.between(Instant.now(), hasta).getSeconds();
        if (s <= 0) { bloqueos.remove(ip); return 0; }
        return s;
    }
}
