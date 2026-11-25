package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.excepcion.AccesoDenegadoException;
import com.mineria.reclutamiento.modelo.*;
import com.mineria.reclutamiento.util.EstandaresIso;
import com.mineria.reclutamiento.repositorio.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacionServicioImpl implements AutenticacionServicio {
    private static final int MAX_INTENTOS = 5;
    private static final int MINUTOS_BLOQUEO = 15;

    private final UsuarioRepositorio usuarios;
    private final PerfilRepositorio perfiles;
    private final EmpresaRepositorio empresas;
    private final AuditoriaServicio auditoria;
    private final PasswordEncoder encoder;

    public AutenticacionServicioImpl(UsuarioRepositorio usuarios, PerfilRepositorio perfiles,
            EmpresaRepositorio empresas, AuditoriaServicio auditoria, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.perfiles = perfiles;
        this.empresas = empresas;
        this.auditoria = auditoria;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public Usuario registrar(String correo, String clave, Rol rol, String nombre, String pais, String idioma, boolean consentimiento) {
        if (rol == Rol.ADMINISTRADOR) throw new AccesoDenegadoException("No se puede registrar un administrador");
        if (!consentimiento) throw new IllegalArgumentException("Debe aceptar el tratamiento de datos personales (Ley 29733 / RGPD)");
        String paisIso = EstandaresIso.pais(pais);
        String idiomaIso = EstandaresIso.idioma(idioma);
        if (clave == null || clave.length() < 12) throw new IllegalArgumentException("La clave debe tener al menos 12 caracteres");
        String c = correo.trim().toLowerCase();
        if (usuarios.existsByCorreo(c)) throw new IllegalStateException("El correo ya esta registrado");

        Usuario u = new Usuario(UUID.randomUUID(), c, rol);
        u.setClaveHash(encoder.encode(clave));
        u.setConsentimientoEn(Instant.now());
        usuarios.save(u);
        if (rol == Rol.TRABAJADOR) perfiles.save(new Perfil(u.getId(), nombre, paisIso, idiomaIso));
        else empresas.save(new Empresa(UUID.randomUUID(), u.getId(), nombre, paisIso));
        auditoria.registrar(u.getId(), "REGISTRO", "usuario", true);
        return u;
    }

    @Override
    @Transactional(noRollbackFor = AuthenticationException.class)
    public Usuario iniciarSesion(String correo, String clave, String codigoTotp) {
        Usuario u = usuarios.findByCorreo(correo.trim().toLowerCase()).orElse(null);
        if (u == null) {
            auditoria.registrar(null, "LOGIN", "correo desconocido", false);
            throw new BadCredentialsException("Credenciales invalidas");
        }
        if (u.getBloqueadoHasta() != null && u.getBloqueadoHasta().isAfter(Instant.now())) {
            auditoria.registrar(u.getId(), "LOGIN_BLOQUEADO", "usuario", false);
            throw new LockedException("Cuenta bloqueada temporalmente");
        }
        if (!encoder.matches(clave, u.getClaveHash())) {
            u.setIntentosFallidos(u.getIntentosFallidos() + 1);
            if (u.getIntentosFallidos() >= MAX_INTENTOS) {
                u.setBloqueadoHasta(Instant.now().plus(MINUTOS_BLOQUEO, ChronoUnit.MINUTES));
                u.setIntentosFallidos(0);
            }
            usuarios.save(u);
            auditoria.registrar(u.getId(), "LOGIN", "usuario", false);
            throw new BadCredentialsException("Credenciales invalidas");
        }
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        usuarios.save(u);
        auditoria.registrar(u.getId(), "LOGIN", "usuario", true);
        return u;
    }

    @Override
    public void validarRol(Usuario usuario, Rol... rolesPermitidos) {
        if (!Arrays.asList(rolesPermitidos).contains(usuario.getRol())) {
            auditoria.registrar(usuario.getId(), "ACCESO_DENEGADO", "rol", false);
            throw new AccesoDenegadoException("Rol sin permiso");
        }
    }
}
