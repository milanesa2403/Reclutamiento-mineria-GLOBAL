package com.mineria.reclutamiento.servicio;

import com.mineria.reclutamiento.modelo.Rol;
import com.mineria.reclutamiento.modelo.Usuario;

public interface AutenticacionServicio {
    Usuario registrar(String correo, String clave, Rol rol, String nombre, String pais, String idioma, boolean consentimiento);
    Usuario iniciarSesion(String correo, String clave, String codigoTotp); // TOTP: etapa posterior
    void validarRol(Usuario usuario, Rol... rolesPermitidos); // lanza AccesoDenegadoException
}
