package com.mineria.reclutamiento.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mineria.reclutamiento.core.config.CognitoProperties;
import com.mineria.reclutamiento.core.security.CognitoAutenticacion;
import com.mineria.reclutamiento.core.security.SecurityConfig;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import com.mineria.reclutamiento.modules.admin.controller.AdminEmpresaController;
import com.mineria.reclutamiento.modules.admin.service.AdminEmpresaService;
import com.mineria.reclutamiento.modules.empresa.controller.EmpresaController;
import com.mineria.reclutamiento.modules.empresa.controller.SolicitudEmpresaController;
import com.mineria.reclutamiento.modules.empresa.entity.EstadoEmpresa;
import com.mineria.reclutamiento.modules.empresa.service.SolicitudEmpresaService;
import com.mineria.reclutamiento.modules.empresa.dto.response.EmpresaPerfilResponse;
import com.mineria.reclutamiento.modules.empresa.service.EmpresaService;
import com.mineria.reclutamiento.modules.postulante.controller.PostulanteController;
import com.mineria.reclutamiento.modules.postulante.dto.request.ActualizarPostulanteRequest;
import com.mineria.reclutamiento.modules.postulante.dto.response.PerfilPostulanteResponse;
import com.mineria.reclutamiento.modules.postulante.entity.DocumentoPostulante;
import com.mineria.reclutamiento.modules.postulante.service.CertificadoArchivoService;
import com.mineria.reclutamiento.modules.postulante.service.DocumentosPostulanteService;
import com.mineria.reclutamiento.modules.postulacion.controller.PostulacionController;
import com.mineria.reclutamiento.modules.convocatoria.controller.BusquedaController;
import com.mineria.reclutamiento.modules.convocatoria.controller.ConvocatoriaEmpresaController;
import com.mineria.reclutamiento.modules.convocatoria.service.BusquedaService;
import com.mineria.reclutamiento.modules.convocatoria.controller.ConvocatoriaGuardadaController;
import com.mineria.reclutamiento.modules.convocatoria.controller.ConvocatoriaPublicaController;
import com.mineria.reclutamiento.modules.convocatoria.service.ConvocatoriaEmpresaService;
import com.mineria.reclutamiento.modules.convocatoria.service.ConvocatoriaGuardadaService;
import com.mineria.reclutamiento.modules.convocatoria.service.ConvocatoriaPublicaService;
import com.mineria.reclutamiento.modules.ubigeo.controller.UbigeoController;
import com.mineria.reclutamiento.modules.ubigeo.repository.UbigeoRepository;
import com.mineria.reclutamiento.modules.postulacion.controller.CandidatoController;
import com.mineria.reclutamiento.modules.postulacion.entity.EstadoPostulacion;
import com.mineria.reclutamiento.modules.postulacion.service.PostulacionService;
import com.mineria.reclutamiento.modules.postulacion.service.CandidatoService;
import com.mineria.reclutamiento.modules.postulante.service.PerfilProfesionalService;
import com.mineria.reclutamiento.modules.postulante.service.PostulanteService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = {
        PostulanteController.class, EmpresaController.class,
        AdminEmpresaController.class, SolicitudEmpresaController.class, PostulacionController.class,
        CandidatoController.class, ConvocatoriaEmpresaController.class, ConvocatoriaPublicaController.class,
        ConvocatoriaGuardadaController.class, UbigeoController.class, BusquedaController.class})
@Import({SecurityConfig.class, CognitoAutenticacion.class})
@EnableConfigurationProperties(CognitoProperties.class)
@TestPropertySource(properties = {
        "app.cognito.issuer-uri=https://cognito-idp.us-east-2.amazonaws.com/us-east-2_prueba",
        "app.cognito.client-id=cliente-prueba",
        "app.cors.allowed-origins=http://localhost:5173"
})
class AutorizacionEndpointsTest {

    private static final String CORREO_POSTULANTE = "postulante@prueba.com";
    private static final String CORREO_EMPRESA = "empresa@prueba.com";

    @Autowired MockMvc mvc;

    @MockitoBean UsuarioRepository usuarioRepository;
    @MockitoBean PostulanteService postulanteService;
    @MockitoBean EmpresaService empresaService;
    @MockitoBean AdminEmpresaService adminEmpresaService;
    @MockitoBean PerfilProfesionalService perfilProfesionalService;
    @MockitoBean SolicitudEmpresaService solicitudEmpresaService;
    @MockitoBean DocumentosPostulanteService documentosPostulanteService;
    @MockitoBean CertificadoArchivoService certificadoArchivoService;
    @MockitoBean PostulacionService postulacionService;
    @MockitoBean CandidatoService candidatoService;
    @MockitoBean com.mineria.reclutamiento.modules.postulacion.service.EntrevistaService entrevistaService;
    @MockitoBean ConvocatoriaEmpresaService convocatoriaEmpresaService;
    @MockitoBean ConvocatoriaPublicaService convocatoriaPublicaService;
    @MockitoBean ConvocatoriaGuardadaService convocatoriaGuardadaService;
    @MockitoBean UbigeoRepository ubigeoRepository;
    @MockitoBean BusquedaService busquedaService;


    private MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder req, String correo, String rol) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").claim("email", correo).claim("token_use", "id")
                .build();
        return req.with(authentication(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol)), correo)));
    }


    @Test
    void postulantes_postulanteNoPuedeVerOtrosPorId() throws Exception {
        mvc.perform(como(get("/api/postulantes/7"), CORREO_POSTULANTE, "POSTULANTE")).andExpect(status().isForbidden());
        mvc.perform(como(get("/api/postulantes/persona/7"), CORREO_POSTULANTE, "POSTULANTE")).andExpect(status().isForbidden());
        verifyNoInteractions(postulanteService);
    }

    @Test
    void me_get_usaLaIdentidadDelToken() throws Exception {
        when(postulanteService.obtenerMiPerfil(CORREO_POSTULANTE)).thenReturn(perfil("987654321"));

        mvc.perform(como(get("/api/postulantes/me"), CORREO_POSTULANTE, "POSTULANTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dni").value("12345678"))
                .andExpect(jsonPath("$.data.correo").value(CORREO_POSTULANTE));

        verify(postulanteService).obtenerMiPerfil(CORREO_POSTULANTE);
    }

    @Test
    void me_soloRolPostulante() throws Exception {
        mvc.perform(como(get("/api/postulantes/me"), CORREO_EMPRESA, "EMPRESA")).andExpect(status().isForbidden());
        mvc.perform(get("/api/postulantes/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(postulanteService);
    }

    @Test
    void me_put_actualizaSoloElPerfilDelToken_eIgnoraDatosOficiales() throws Exception {
        when(postulanteService.actualizarMiPerfil(eq(CORREO_POSTULANTE), any())).thenReturn(perfil("911222333"));

        mvc.perform(como(put("/api/postulantes/me"), CORREO_POSTULANTE, "POSTULANTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telefono":"911 222 333","direccion":"Av. Minera 123","resumenProfesional":"Operador",
                                 "dni":"99999999","nombres":"Otro","apellidos":"Nombre","idPersona":1}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Perfil actualizado correctamente"))
                .andExpect(jsonPath("$.data.telefono").value("911222333"));

        ArgumentCaptor<ActualizarPostulanteRequest> captor = ArgumentCaptor.forClass(ActualizarPostulanteRequest.class);
        verify(postulanteService).actualizarMiPerfil(eq(CORREO_POSTULANTE), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().telefono()).isEqualTo("911 222 333");
    }

    @Test
    void me_put_validaEnBackend() throws Exception {
        mvc.perform(como(put("/api/postulantes/me"), CORREO_POSTULANTE, "POSTULANTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefono\":\"abc\",\"direccion\":null,\"resumenProfesional\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.telefono").exists());

        mvc.perform(como(put("/api/postulantes/me"), CORREO_POSTULANTE, "POSTULANTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefono\":\"987654321\",\"resumenProfesional\":\"" + "x".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.resumenProfesional").exists());

        mvc.perform(como(put("/api/postulantes/me"), CORREO_POSTULANTE, "POSTULANTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefono\":\"987654321\",\"direccion\":\"" + "x".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.direccion").exists());

        verifyNoInteractions(postulanteService);
    }


    @Test
    void empresaPerfil_soloRolEmpresa() throws Exception {
        mvc.perform(como(get("/api/empresa/perfil"), CORREO_POSTULANTE, "POSTULANTE")).andExpect(status().isForbidden());
        verifyNoInteractions(empresaService);
    }

    @Test
    void empresaPerfil_usaLaEmpresaDelToken() throws Exception {
        when(empresaService.perfilDe(CORREO_EMPRESA)).thenReturn(new EmpresaPerfilResponse(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        mvc.perform(como(get("/api/empresa/perfil"), CORREO_EMPRESA, "EMPRESA")).andExpect(status().isOk());
        verify(empresaService).perfilDe(CORREO_EMPRESA);
    }

    @Test
    void empresaPerfil_put_validaCamposObligatoriosYLargos() throws Exception {
        mvc.perform(como(put("/api/empresa/perfil"), CORREO_EMPRESA, "EMPRESA")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cargoRepresentante\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.cargoRepresentante").exists());

        mvc.perform(como(put("/api/empresa/perfil"), CORREO_EMPRESA, "EMPRESA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"" + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.descripcion").exists());

        mvc.perform(como(put("/api/empresa/perfil"), CORREO_EMPRESA, "EMPRESA")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.telefono").exists());

        mvc.perform(como(put("/api/empresa/perfil"), CORREO_EMPRESA, "EMPRESA")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"telefonoRepresentante\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.telefonoRepresentante").exists());

        verifyNoInteractions(empresaService);
    }

    @Test
    void empresaPerfil_put_parcialEsValido() throws Exception {
        when(empresaService.actualizarPerfil(eq(CORREO_EMPRESA), any())).thenReturn(new EmpresaPerfilResponse(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        mvc.perform(como(put("/api/empresa/perfil"), CORREO_EMPRESA, "EMPRESA")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"descripcion\":\"Nueva descripción\"}"))
                .andExpect(status().isOk());
        verify(empresaService).actualizarPerfil(eq(CORREO_EMPRESA), any());
    }


    @Test
    void adminEmpresas_soloAdministrador() throws Exception {
        mvc.perform(como(get("/api/admin/empresas"), CORREO_EMPRESA, "EMPRESA")).andExpect(status().isForbidden());
        mvc.perform(como(get("/api/admin/empresas"), CORREO_POSTULANTE, "POSTULANTE")).andExpect(status().isForbidden());
        mvc.perform(como(post("/api/admin/empresas/10/aprobar"), CORREO_EMPRESA, "EMPRESA")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/empresas")).andExpect(status().isUnauthorized());
        verifyNoInteractions(adminEmpresaService);
    }

    @Test
    void adminEmpresas_listarConFiltroDeEstado() throws Exception {
        when(adminEmpresaService.listar(EstadoEmpresa.EN_REVISION)).thenReturn(java.util.List.of());
        mvc.perform(como(get("/api/admin/empresas?estado=EN_REVISION"), "admin@prueba.com", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mvc.perform(como(get("/api/admin/empresas?estado=INVENTADO"), "admin@prueba.com", "ADMINISTRADOR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminEmpresas_observarYRechazar_exigenMotivo() throws Exception {
        mvc.perform(como(post("/api/admin/empresas/10/rechazar"), "admin@prueba.com", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.motivo").exists());
        mvc.perform(como(post("/api/admin/empresas/10/observar"), "admin@prueba.com", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(adminEmpresaService);
    }

    @Test
    void adminEmpresas_aprobarUsaElAdminDelToken() throws Exception {
        mvc.perform(como(post("/api/admin/empresas/10/aprobar"), "admin@prueba.com", "ADMINISTRADOR"))
                .andExpect(status().isOk());
        verify(adminEmpresaService).aprobar(10L, "admin@prueba.com");
    }


    @Test
    void solicitudEmpresa_soloRolEmpresa_yUsaLaEmpresaDelToken() throws Exception {
        mvc.perform(como(get("/api/empresa/solicitud"), CORREO_POSTULANTE, "POSTULANTE")).andExpect(status().isForbidden());
        mvc.perform(como(get("/api/empresa/solicitud"), "admin@prueba.com", "ADMINISTRADOR")).andExpect(status().isForbidden());

        mvc.perform(como(get("/api/empresa/solicitud"), CORREO_EMPRESA, "EMPRESA")).andExpect(status().isOk());
        verify(solicitudEmpresaService).obtenerMiSolicitud(CORREO_EMPRESA);
    }

    @Test
    void solicitudEmpresa_subirDocumento_esMultipart_yDeLaEmpresaDelToken() throws Exception {
        org.springframework.mock.web.MockMultipartFile archivo = new org.springframework.mock.web.MockMultipartFile(
                "archivo", "poder.pdf", "application/pdf", "%PDF-1.7".getBytes());

        mvc.perform(como(multipart("/api/empresa/solicitud/documento").file(archivo), CORREO_EMPRESA, "EMPRESA"))
                .andExpect(status().isOk());
        verify(solicitudEmpresaService).subirAcreditacion(eq(CORREO_EMPRESA), any());

        mvc.perform(como(multipart("/api/empresa/solicitud/documento").file(archivo), CORREO_POSTULANTE, "POSTULANTE"))
                .andExpect(status().isForbidden());
    }


    @Test
    void documentosPostulante_losCuatroTipos_soloDelPostulanteDelToken() throws Exception {
        org.springframework.mock.web.MockMultipartFile archivo = new org.springframework.mock.web.MockMultipartFile(
                "archivo", "doc.pdf", "application/pdf", "%PDF-1.7".getBytes());

        for (DocumentoPostulante tipo : DocumentoPostulante.values()) {
            String ruta = "/api/postulantes/me/documentos/" + tipo.ruta();
            mvc.perform(como(multipart(ruta).file(archivo), CORREO_POSTULANTE, "POSTULANTE")).andExpect(status().isOk());
            verify(documentosPostulanteService).subir(eq(CORREO_POSTULANTE), eq(tipo), any());
            mvc.perform(como(get(ruta + "/url"), CORREO_POSTULANTE, "POSTULANTE")).andExpect(status().isOk());
            verify(documentosPostulanteService).url(CORREO_POSTULANTE, tipo);

            mvc.perform(como(multipart(ruta).file(archivo), CORREO_EMPRESA, "EMPRESA")).andExpect(status().isForbidden());
            mvc.perform(como(get(ruta + "/url"), CORREO_EMPRESA, "EMPRESA")).andExpect(status().isForbidden());
            mvc.perform(multipart(ruta).file(archivo)).andExpect(status().isUnauthorized());
        }
        mvc.perform(como(get("/api/postulantes/me/documentos/pasaporte/url"), CORREO_POSTULANTE, "POSTULANTE"))
                .andExpect(status().isNotFound());
        mvc.perform(como(multipart("/api/postulantes/99/cv").file(archivo), CORREO_POSTULANTE, "POSTULANTE"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                        .isIn(403, 404, 405));
    }

    @Test
    void pdfCertificacion_soloElPostulante_conElCorreoDelToken() throws Exception {
        org.springframework.mock.web.MockMultipartFile archivo = new org.springframework.mock.web.MockMultipartFile(
                "archivo", "sctr.pdf", "application/pdf", "%PDF-1.7".getBytes());

        mvc.perform(como(multipart("/api/postulantes/me/certificaciones/5/archivo").file(archivo),
                CORREO_POSTULANTE, "POSTULANTE")).andExpect(status().isOk());
        verify(certificadoArchivoService).subir(eq(CORREO_POSTULANTE), eq(5L), any());
        mvc.perform(como(get("/api/postulantes/me/certificaciones/5/archivo/url"), CORREO_POSTULANTE, "POSTULANTE"))
                .andExpect(status().isOk());
        verify(certificadoArchivoService).url(CORREO_POSTULANTE, 5L);

        mvc.perform(como(multipart("/api/postulantes/me/certificaciones/5/archivo").file(archivo),
                CORREO_EMPRESA, "EMPRESA")).andExpect(status().isForbidden());
    }


    @Test
    void postular_soloPostulante_yConElCorreoDelToken() throws Exception {
        String cuerpo = "{\"idConvocatoria\":5,\"idsCertificaciones\":[1]}";
        mvc.perform(como(post("/api/postulaciones").contentType(MediaType.APPLICATION_JSON).content(cuerpo),
                CORREO_POSTULANTE, "POSTULANTE")).andExpect(status().isCreated());
        verify(postulacionService).postular(eq(CORREO_POSTULANTE), any());
        mvc.perform(como(get("/api/postulaciones/mias"), CORREO_POSTULANTE, "POSTULANTE")).andExpect(status().isOk());
        mvc.perform(como(get("/api/postulaciones/requisitos"), CORREO_POSTULANTE, "POSTULANTE")).andExpect(status().isOk());

        mvc.perform(como(post("/api/postulaciones").contentType(MediaType.APPLICATION_JSON).content(cuerpo),
                CORREO_EMPRESA, "EMPRESA")).andExpect(status().isForbidden());
        mvc.perform(post("/api/postulaciones").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());
        mvc.perform(como(post("/api/postulaciones").contentType(MediaType.APPLICATION_JSON).content("{}"),
                CORREO_POSTULANTE, "POSTULANTE")).andExpect(status().isBadRequest());
    }

    @Test
    void revisionDeCandidatos_soloEmpresa_yConElCorreoDelToken() throws Exception {
        mvc.perform(como(get("/api/empresa/convocatorias/5/postulaciones"), CORREO_EMPRESA, "EMPRESA"))
                .andExpect(status().isOk());
        verify(candidatoService).candidatos(CORREO_EMPRESA, 5L);
        mvc.perform(como(patch("/api/empresa/postulaciones/100/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"ACEPTADO\"}"), CORREO_EMPRESA, "EMPRESA")).andExpect(status().isOk());
        verify(candidatoService).cambiarEstado(CORREO_EMPRESA, 100L, EstadoPostulacion.ACEPTADO);
        mvc.perform(como(get("/api/empresa/postulaciones/100/documentos/cv/url"), CORREO_EMPRESA, "EMPRESA"))
                .andExpect(status().isOk());

        mvc.perform(como(get("/api/empresa/convocatorias/5/postulaciones"), CORREO_POSTULANTE, "POSTULANTE"))
                .andExpect(status().isForbidden());
        mvc.perform(como(patch("/api/empresa/postulaciones/100/estado").contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"CONTRATADO\"}"), CORREO_EMPRESA, "EMPRESA")).andExpect(status().isBadRequest());
    }


    @Test
    void convocatoriasDeLaEmpresa_soloEmpresa_conLaEmpresaDelToken() throws Exception {
        String cuerpo = "{\"tipo\":\"PLANILLA_COMPLETA\",\"puesto\":\"Operador\",\"campamento\":\"Antamina\","
                + "\"turno\":\"14x7\",\"descripcion\":\"d\",\"requisitosObligatorios\":\"r\",\"codigoUbigeo\":\"020101\"}";
        mvc.perform(como(post("/api/empresa/convocatorias").contentType(MediaType.APPLICATION_JSON).content(cuerpo),
                CORREO_EMPRESA, "EMPRESA")).andExpect(status().isCreated());
        verify(convocatoriaEmpresaService).crear(eq(CORREO_EMPRESA), any());
        mvc.perform(como(post("/api/empresa/convocatorias/5/publicar"), CORREO_EMPRESA, "EMPRESA")).andExpect(status().isOk());
        verify(convocatoriaEmpresaService).publicar(CORREO_EMPRESA, 5L);

        mvc.perform(como(post("/api/empresa/convocatorias").contentType(MediaType.APPLICATION_JSON).content(cuerpo),
                CORREO_POSTULANTE, "POSTULANTE")).andExpect(status().isForbidden());
        mvc.perform(get("/api/empresa/convocatorias")).andExpect(status().isUnauthorized());
        mvc.perform(como(post("/api/empresa/convocatorias").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo.replace("\"codigoUbigeo\":\"020101\"", "\"codigoUbigeo\":\"\"")), CORREO_EMPRESA, "EMPRESA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void busquedas_soloEmpresa() throws Exception {
        mvc.perform(como(post("/api/empresa/busquedas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Parada octubre\"}"), CORREO_EMPRESA, "EMPRESA")).andExpect(status().isCreated());
        verify(busquedaService).crear(CORREO_EMPRESA, "Parada octubre");
        mvc.perform(como(post("/api/empresa/busquedas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\" \"}"), CORREO_EMPRESA, "EMPRESA")).andExpect(status().isBadRequest());
        mvc.perform(como(get("/api/empresa/busquedas"), CORREO_POSTULANTE, "POSTULANTE")).andExpect(status().isForbidden());

        mvc.perform(como(put("/api/empresa/convocatorias/5/busqueda").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idBusqueda\":null}"), CORREO_EMPRESA, "EMPRESA")).andExpect(status().isOk());
        verify(convocatoriaEmpresaService).mover(CORREO_EMPRESA, 5L, null);
    }

    @Test
    void feedPublico_yUbigeo_sinLogin() throws Exception {
        mvc.perform(get("/api/public/convocatorias").param("tipo", "PARADA_PLANTA").param("departamento", "12"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/public/convocatorias/5")).andExpect(status().isOk());
        mvc.perform(get("/api/public/ubigeo/departamentos")).andExpect(status().isOk());
        mvc.perform(get("/api/public/ubigeo/departamentos/12/provincias")).andExpect(status().isOk());
        mvc.perform(get("/api/public/ubigeo/departamentos/1/provincias")).andExpect(status().isBadRequest());
    }

    @Test
    void convocatoriasGuardadas_soloPostulante() throws Exception {
        mvc.perform(como(put("/api/postulantes/me/convocatorias-guardadas/5"), CORREO_POSTULANTE, "POSTULANTE"))
                .andExpect(status().isOk());
        verify(convocatoriaGuardadaService).guardar(CORREO_POSTULANTE, 5L);
        mvc.perform(como(put("/api/postulantes/me/convocatorias-guardadas/5"), CORREO_EMPRESA, "EMPRESA"))
                .andExpect(status().isForbidden());
    }

    private static final String PERFIL_VALIDO = """
            {"resumenProfesional":"Operador con 5 años en mina subterránea",
             "especialidades":[{"nombre":"Operación de maquinaria pesada","principal":true}],
             "habilidades":["Scoop","Jumbo"],
             "experiencias":[{"puesto":"Operador","empresa":"Minera Sur","fechaInicio":"2020-01-01","fechaFin":null,"descripcion":null}],
             "formaciones":[], "certificaciones":[],
             "idPostulante":99, "cvUrl":"data:application/pdf;base64,AAAA"}""";

    @Test
    void perfilProfesional_soloRolPostulante_yDelToken() throws Exception {
        mvc.perform(como(put("/api/postulantes/me/perfil"), CORREO_EMPRESA, "EMPRESA")
                        .contentType(MediaType.APPLICATION_JSON).content(PERFIL_VALIDO))
                .andExpect(status().isForbidden());
        mvc.perform(como(get("/api/postulantes/me/perfil"), "admin@prueba.com", "ADMINISTRADOR"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/postulantes/me/perfil")).andExpect(status().isUnauthorized());
        verifyNoInteractions(perfilProfesionalService);

        mvc.perform(como(put("/api/postulantes/me/perfil"), CORREO_POSTULANTE, "POSTULANTE")
                        .contentType(MediaType.APPLICATION_JSON).content(PERFIL_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Perfil profesional creado correctamente"));
        verify(perfilProfesionalService).reemplazar(eq(CORREO_POSTULANTE), any());
    }

    @Test
    void perfilProfesional_validaEnBackend_conErroresPorCampo() throws Exception {
        String sinHabilidadesNiEspecialidad = """
                {"especialidades":[], "habilidades":[],
                 "experiencias":[{"puesto":"","empresa":"X","fechaInicio":"2999-01-01"}]}""";
        mvc.perform(como(put("/api/postulantes/me/perfil"), CORREO_POSTULANTE, "POSTULANTE")
                        .contentType(MediaType.APPLICATION_JSON).content(sinHabilidadesNiEspecialidad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.especialidades").exists())
                .andExpect(jsonPath("$.data.habilidades").exists())
                .andExpect(jsonPath("$.data['experiencias[0].puesto']").exists())
                .andExpect(jsonPath("$.data['experiencias[0].fechaInicio']").exists());

        String demasiadoLargo = PERFIL_VALIDO.replace("\"Scoop\"", "\"" + "x".repeat(101) + "\"");
        mvc.perform(como(put("/api/postulantes/me/perfil"), CORREO_POSTULANTE, "POSTULANTE")
                        .contentType(MediaType.APPLICATION_JSON).content(demasiadoLargo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data['habilidades[0]']").exists());

        String veintiunHabilidades = PERFIL_VALIDO.replace("[\"Scoop\",\"Jumbo\"]",
                java.util.stream.IntStream.rangeClosed(1, 21).mapToObj(i -> "\"h" + i + "\"")
                        .collect(java.util.stream.Collectors.joining(",", "[", "]")));
        mvc.perform(como(put("/api/postulantes/me/perfil"), CORREO_POSTULANTE, "POSTULANTE")
                        .contentType(MediaType.APPLICATION_JSON).content(veintiunHabilidades))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.habilidades").exists());
        verifyNoInteractions(perfilProfesionalService);
    }

    private static PerfilPostulanteResponse perfil(String telefono) {
        return new PerfilPostulanteResponse("PE", "DNI", "12345678", "JUAN", "PEREZ", CORREO_POSTULANTE, telefono,
                null, null, null, true, java.util.List.of());
    }
}
