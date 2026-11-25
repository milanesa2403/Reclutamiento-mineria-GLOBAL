package com.mineria.reclutamiento;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlujoTest {
    @Autowired MockMvc mvc;

    private static String login(String correo, String clave) {
        return "{\"correo\":\"" + correo + "\",\"clave\":\"" + clave + "\"}";
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/v1/yo")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminPuedeIniciarSesion() throws Exception {
        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login("admin@mineria.com", "AdminMineria123!")))
            .andExpect(status().isOk());
    }

    @Test
    void claveIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login("admin@mineria.com", "incorrecta")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void noSePuedeRegistrarAdministrador() throws Exception {
        String body = "{\"correo\":\"x@y.com\",\"clave\":\"ClaveSegura123!\",\"rol\":\"ADMINISTRADOR\","
                + "\"nombre\":\"X\",\"pais\":\"PE\",\"idioma\":\"es\",\"consentimiento\":true}";
        mvc.perform(post("/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }
}
