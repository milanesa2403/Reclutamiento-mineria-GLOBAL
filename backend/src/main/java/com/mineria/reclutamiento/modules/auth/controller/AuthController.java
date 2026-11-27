package com.mineria.reclutamiento.modules.auth.controller;

import com.mineria.reclutamiento.core.security.CognitoAutenticacion;
import com.mineria.reclutamiento.core.validacion.DocumentoIdentidad;
import com.mineria.reclutamiento.modules.auth.entity.Rol;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.auth.repository.UsuarioRepository;
import com.mineria.reclutamiento.modules.empresa.dto.request.EmpresaRegistroRequest;
import com.mineria.reclutamiento.modules.empresa.repository.EmpresaRepository;
import com.mineria.reclutamiento.modules.empresa.service.EmpresaService;
import com.mineria.reclutamiento.modules.persona.dto.IdentidadPersona;
import com.mineria.reclutamiento.modules.persona.repository.PersonaRepository;
import com.mineria.reclutamiento.modules.postulante.dto.request.PostulanteRequest;
import com.mineria.reclutamiento.modules.postulante.service.PostulanteService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaService empresaService;
    private final PostulanteService postulanteService;
    private final EmpresaRepository empresaRepository;
    private final PersonaRepository personaRepository;
    private final TransactionTemplate transaccion;

    public AuthController(
            UsuarioRepository usuarioRepository,
            EmpresaService empresaService,
            PostulanteService postulanteService,
            EmpresaRepository empresaRepository,
            PersonaRepository personaRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.usuarioRepository = usuarioRepository;
        this.empresaService = empresaService;
        this.postulanteService = postulanteService;
        this.empresaRepository = empresaRepository;
        this.personaRepository = personaRepository;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    @GetMapping("/yo")
    public ResponseEntity<?> yo(@AuthenticationPrincipal Jwt jwt) {
        String correo = CognitoAutenticacion.correoDe(jwt);
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);
        if (usuario == null && CognitoAutenticacion.esAdministrador(jwt)) {
            usuario = usuarioRepository.save(new Usuario(correo, Rol.ADMINISTRADOR));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("correo", correo);
        data.put("rol", usuario != null && usuario.isCuentaActiva() ? usuario.getRol().name() : null);
        return ResponseEntity.ok(Map.of("mensaje", "Sesión válida", "data", data));
    }

    @PostMapping("/register/postulante")
    public ResponseEntity<?> registrarPostulante(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PostulanteRequest request
    ) {
        String correo = CognitoAutenticacion.correoDe(jwt);
        String telefono = request.telefono().trim();

        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El correo electrónico ya está registrado."));
        }

        IdentidadPersona identidad = new IdentidadPersona(
                DocumentoIdentidad.normalizar(request.pais()),
                request.tipoDocumento(),
                DocumentoIdentidad.normalizar(request.numeroDocumento()),
                request.nombres().trim(),
                request.apellidos().trim());

        String problemaPais = DocumentoIdentidad.problemaPais(identidad.pais());
        if (problemaPais != null) {
            return error("pais", problemaPais);
        }

        String problemaDocumento = DocumentoIdentidad.problemaDocumento(
                identidad.pais(), identidad.tipoDocumento(), identidad.numeroDocumento());
        if (problemaDocumento != null) {
            return error("numeroDocumento", problemaDocumento);
        }

        if (personaRepository.existsByPaisAndTipoDocumentoAndDni(
                identidad.pais(), identidad.tipoDocumento(), identidad.numeroDocumento())) {
            return error("numeroDocumento", "Este documento ya se encuentra registrado.");
        }

        if (personaRepository.existsByTelefono(telefono)) {
            return ResponseEntity.badRequest().body(
                    Map.of("mensaje", "El teléfono ya se encuentra registrado.")
            );
        }

        transaccion.executeWithoutResult(status -> {
            Usuario nuevo = usuarioRepository.save(new Usuario(correo, Rol.POSTULANTE));
            postulanteService.registrarDatos(nuevo.getId(), request, identidad);
        });

        return ResponseEntity.ok(
                Map.of(
                        "mensaje", "Postulante registrado correctamente.",
                        "correo", correo
                )
        );
    }

    @PostMapping("/register/empresa")
    public ResponseEntity<?> registrarEmpresa(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EmpresaRegistroRequest request
    ) {
        String correo = CognitoAutenticacion.correoDe(jwt);

        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("correo", "El correo electrónico ya está registrado."));
        }

        String pais = DocumentoIdentidad.normalizar(request.pais());
        String identificador = DocumentoIdentidad.normalizar(request.identificadorFiscal());
        IdentidadPersona representante = new IdentidadPersona(
                DocumentoIdentidad.normalizar(request.paisRepresentante()),
                request.tipoDocumentoRepresentante(),
                DocumentoIdentidad.normalizar(request.numeroDocumentoRepresentante()),
                request.nombresRepresentante().trim(),
                request.apellidosRepresentante().trim());

        String problemaPais = DocumentoIdentidad.problemaPais(pais);
        if (problemaPais != null) {
            return error("pais", problemaPais);
        }

        String problemaFiscal = DocumentoIdentidad.problemaIdentificadorFiscal(pais, identificador);
        if (problemaFiscal != null) {
            return error("identificadorFiscal", problemaFiscal);
        }

        String problemaPaisRepresentante = DocumentoIdentidad.problemaPais(representante.pais());
        if (problemaPaisRepresentante != null) {
            return error("paisRepresentante", problemaPaisRepresentante);
        }

        String problemaDocumento = DocumentoIdentidad.problemaDocumento(
                representante.pais(), representante.tipoDocumento(), representante.numeroDocumento());
        if (problemaDocumento != null) {
            return error("numeroDocumentoRepresentante", problemaDocumento);
        }

        if (empresaService.existeIdentificadorFiscal(pais, identificador)) {
            return error("identificadorFiscal", "Esta empresa ya se encuentra registrada en el sistema.");
        }

        if (empresaService.existeDocumentoRepresentante(representante)) {
            return error("numeroDocumentoRepresentante", "Este documento ya está asociado a otra cuenta.");
        }

        if (request.telefono() != null && !request.telefono().isBlank()
                && empresaRepository.existsByTelefono(request.telefono().trim())) {
            return ResponseEntity.badRequest().body(
                    Map.of("telefono", "Este teléfono de empresa ya se encuentra registrado.")
            );
        }

        if (personaRepository.existsByTelefono(request.telefonoRepresentante().trim())) {
            return ResponseEntity.badRequest().body(
                    Map.of("telefonoRepresentante", "Este teléfono ya está asociado a otra persona.")
            );
        }

        transaccion.executeWithoutResult(status -> {
            Usuario nuevo = usuarioRepository.save(new Usuario(correo, Rol.EMPRESA));
            empresaService.registrarDatos(nuevo.getId(), correo, request, representante);
        });

        return ResponseEntity.ok(
                Map.of("mensaje", "Empresa registrada correctamente. Sube tu documento de acreditación para la revisión.")
        );
    }

    private static ResponseEntity<Map<String, String>> error(String campo, String mensaje) {
        return ResponseEntity.badRequest().body(Map.of(campo, mensaje, "mensaje", mensaje));
    }
}
