package com.mineria.reclutamiento.repositorio;

import com.mineria.reclutamiento.modelo.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    java.util.List<Usuario> findByBorradoSolicitadoEnBefore(java.time.Instant limite);
}
