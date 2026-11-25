package com.mineria.reclutamiento.modelo;

import jakarta.persistence.*;
import java.time.Instant;

/** Si llega la misma clave dos veces, se devuelve el resultado guardado sin repetir la operacion. */
@Entity
@Table(name = "operaciones_idempotentes")
public class OperacionIdempotente {
    @Id
    private String clave;
    private String resultado;
    private Instant fecha = Instant.now();

    protected OperacionIdempotente() {}

    public OperacionIdempotente(String clave, String resultado) {
        this.clave = clave;
        this.resultado = resultado;
    }

    public String getClave() { return clave; }
    public String getResultado() { return resultado; }
}
