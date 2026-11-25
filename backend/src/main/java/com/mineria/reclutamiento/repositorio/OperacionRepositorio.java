package com.mineria.reclutamiento.repositorio;

import com.mineria.reclutamiento.modelo.OperacionIdempotente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacionRepositorio extends JpaRepository<OperacionIdempotente, String> {
}
