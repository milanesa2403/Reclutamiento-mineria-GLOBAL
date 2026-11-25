package com.mineria.reclutamiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** Flujo completo: empresa publica, trabajador sube CV y postula, empresa gestiona. Prueba roles, idempotencia e importacion. */
@SpringBootTest(properties = "app.almacen.ruta=target/almacen-test")
@AutoConfigureMockMvc
class FlujoReclutamientoTest {
    static final String CLAVE = "ClaveSegura123!";
    static final byte[] PDF = "%PDF-1.4 contenido de prueba".getBytes();
    @Autowired MockMvc mvc;

    private String registrarYLogin(String rol) throws Exception {
        String correo = rol.toLowerCase() + "-" + UUID.randomUUID() + "@prueba.com";
        mvc.perform(post("/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(
                "{\"correo\":\"" + correo + "\",\"clave\":\"" + CLAVE + "\",\"rol\":\"" + rol
                + "\",\"nombre\":\"Nombre " + rol + "\",\"pais\":\"PE\",\"idioma\":\"es\",\"consentimiento\":true}"))
            .andExpect(status().isCreated());
        String r = mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"clave\":\"" + CLAVE + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(r, "$.token");
    }

    private String publicarOferta(String empresa) throws Exception {
        String r = mvc.perform(post("/v1/empresa/ofertas").header("Authorization", empresa)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Operador de perforacion\",\"descripcion\":\"Parada de planta\","
                        + "\"especialidadRequerida\":\"perforacion\",\"pais\":\"CL\",\"salario\":3500,\"moneda\":\"USD\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(r, "$.id");
    }

    private String subirCv(String trabajador) throws Exception {
        String r = mvc.perform(multipart("/v1/documentos")
                .file(new MockMultipartFile("archivo", "cv.pdf", "application/pdf", PDF))
                .param("tipo", "CV").header("Authorization", trabajador))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(r, "$.id");
    }

    @Test
    void flujoCompletoConIdempotenciaYRoles() throws Exception {
        String empresa = registrarYLogin("EMPRESA");
        String trabajador = registrarYLogin("TRABAJADOR");
        String ofertaId = publicarOferta(empresa);
        String cvId = subirCv(trabajador);

        mvc.perform(get("/v1/ofertas").param("especialidad", "perfor").param("pais", "CL").header("Authorization", trabajador))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].titulo").exists());

        String body = "{\"ofertaId\":\"" + ofertaId + "\",\"cvId\":\"" + cvId + "\"}";
        String r1 = mvc.perform(post("/v1/trabajador/postulaciones").header("Authorization", trabajador)
                .header("Idempotency-Key", "k1").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        // misma clave -> misma postulacion; otra clave + misma oferta -> tampoco duplica
        String r2 = mvc.perform(post("/v1/trabajador/postulaciones").header("Authorization", trabajador)
                .header("Idempotency-Key", "k1").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String r3 = mvc.perform(post("/v1/trabajador/postulaciones").header("Authorization", trabajador)
                .header("Idempotency-Key", "k2").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String postId = JsonPath.read(r1, "$.id");
        assertEquals(postId, JsonPath.read(r2, "$.id"));
        assertEquals(postId, JsonPath.read(r3, "$.id"));
        mvc.perform(get("/v1/empresa/ofertas/" + ofertaId + "/postulaciones").header("Authorization", empresa))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        // la empresa duena ve el CV; otra empresa NO (403)
        mvc.perform(get("/v1/documentos/" + cvId).header("Authorization", empresa)).andExpect(status().isOk());
        String otraEmpresa = registrarYLogin("EMPRESA");
        mvc.perform(get("/v1/documentos/" + cvId).header("Authorization", otraEmpresa)).andExpect(status().isForbidden());
        mvc.perform(post("/v1/empresa/ofertas/" + ofertaId + "/cerrar").header("Authorization", otraEmpresa))
            .andExpect(status().isForbidden());

        // la empresa cambia el estado y chatea
        mvc.perform(put("/v1/empresa/postulaciones/" + postId + "/estado").header("Authorization", empresa)
                .header("Idempotency-Key", "e1").contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENTREVISTA\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ENTREVISTA"));
        mvc.perform(post("/v1/postulaciones/" + postId + "/mensajes").header("Authorization", empresa)
                .header("Idempotency-Key", "m1").contentType(MediaType.APPLICATION_JSON).content("{\"contenido\":\"Hola, agendemos\"}"))
            .andExpect(status().isCreated());
        mvc.perform(get("/v1/postulaciones/" + postId + "/mensajes").header("Authorization", otraEmpresa))
            .andExpect(status().isForbidden());
    }

    @Test
    void trabajadorNoAccedeARecursosDeEmpresa() throws Exception {
        String trabajador = registrarYLogin("TRABAJADOR");
        mvc.perform(get("/v1/empresa/ofertas").header("Authorization", trabajador)).andExpect(status().isForbidden());
        mvc.perform(get("/v1/admin/auditoria").header("Authorization", trabajador)).andExpect(status().isForbidden());
    }

    @Test
    void importacionCsvReportaFilasInvalidas() throws Exception {
        String empresa = registrarYLogin("EMPRESA");
        String csv = "titulo,descripcion,especialidadRequerida,pais,salario,moneda\n"
                + "Electricista,Mina,electricidad,PE,3000,PEN\n"
                + "Mecanico,Mina,mecanica,XX,2800,USD\n"
                + "Geologo,Mina,geologia,CL,abc,USD\n";
        mvc.perform(multipart("/v1/empresa/ofertas/importar")
                .file(new MockMultipartFile("archivo", "ofertas.csv", "text/csv", csv.getBytes()))
                .param("formato", "csv").header("Authorization", empresa))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.importados").value(1))
            .andExpect(jsonPath("$.errores[0].fila").value(3))
            .andExpect(jsonPath("$.errores[1].fila").value(4));
    }

    @Test
    void erroresUsanFormatoEstandarYVersionRetiradaDevuelve410() throws Exception {
        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").exists()).andExpect(jsonPath("$.mensaje").exists());
        mvc.perform(get("/v0/ofertas")).andExpect(status().isGone()).andExpect(jsonPath("$.versionVigente").value("v1"));
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void modoSoloLecturaEncolaPostulacionesYLasProcesaAlVolver() throws Exception {
        String admin = "Bearer " + JsonPath.read(mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"admin@mineria.com\",\"clave\":\"AdminMineria123!\"}"))
                .andReturn().getResponse().getContentAsString(), "$.token");
        String empresa = registrarYLogin("EMPRESA");
        String trabajador = registrarYLogin("TRABAJADOR");
        String ofertaId = publicarOferta(empresa);
        String cvId = subirCv(trabajador);

        mvc.perform(post("/v1/admin/modo-solo-lectura").param("activo", "true").header("Authorization", admin)).andExpect(status().isOk());
        try {
            mvc.perform(post("/v1/trabajador/postulaciones").header("Authorization", trabajador).header("Idempotency-Key", "ro1")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"ofertaId\":\"" + ofertaId + "\",\"cvId\":\"" + cvId + "\"}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.estado").value("EN_PROCESO"));
            mvc.perform(post("/v1/empresa/ofertas").header("Authorization", empresa).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"titulo\":\"x\",\"pais\":\"PE\",\"salario\":1,\"moneda\":\"USD\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.codigo").value("MANTENIMIENTO"));
        } finally {
            mvc.perform(post("/v1/admin/modo-solo-lectura").param("activo", "false").header("Authorization", admin));
        }
        mvc.perform(get("/v1/admin/cola").header("Authorization", admin)).andExpect(status().isOk());
    }
}
