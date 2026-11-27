package com.mineria.reclutamiento.core.health;

import com.mineria.reclutamiento.core.response.ApiResponse;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hora", OffsetDateTime.now().toString());
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            data.put("baseDeDatos", "UP");
            return ResponseEntity.ok(ApiResponse.ok("Backend y base de datos operativos", data));
        } catch (DataAccessException e) {
            data.put("baseDeDatos", "DOWN");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiResponse<>(
                    false, "El backend responde, pero no puede conectar con la base de datos", data));
        }
    }
}
