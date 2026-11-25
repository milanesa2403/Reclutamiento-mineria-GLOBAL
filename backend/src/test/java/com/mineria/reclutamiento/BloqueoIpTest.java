package com.mineria.reclutamiento;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/** RNF 11: mas de 10 intentos fallidos desde una IP -> 429 con Retry-After. */
@SpringBootTest(properties = "seguridad.ip.max-fallos=10")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BloqueoIpTest {
    @Autowired MockMvc mvc;

    @Test
    void bloqueaIpTrasDemasiadosFallos() throws Exception {
        String ip = "203.0.113.77";
        String body = "{\"correo\":\"admin@mineria.com\",\"clave\":\"incorrecta\"}";
        for (int i = 0; i < 11; i++) {
            mvc.perform(post("/v1/auth/login").header("X-Forwarded-For", ip)
                    .contentType(MediaType.APPLICATION_JSON).content(body));
        }
        mvc.perform(get("/api/health").header("X-Forwarded-For", ip))
            .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        mvc.perform(get("/api/health").header("X-Forwarded-For", "198.51.100.5")).andExpect(status().isOk());
    }
}
