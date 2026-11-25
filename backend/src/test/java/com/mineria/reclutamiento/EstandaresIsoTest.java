package com.mineria.reclutamiento;

import static org.junit.jupiter.api.Assertions.*;

import com.mineria.reclutamiento.util.EstandaresIso;
import org.junit.jupiter.api.Test;

class EstandaresIsoTest {
    @Test
    void aceptaCodigosValidosYNormaliza() {
        assertEquals("PE", EstandaresIso.pais(" pe "));
        assertEquals("es", EstandaresIso.idioma("ES"));
        assertEquals("USD", EstandaresIso.moneda("usd"));
    }

    @Test
    void rechazaCodigosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> EstandaresIso.pais("XX1"));
        assertThrows(IllegalArgumentException.class, () -> EstandaresIso.pais(null));
        assertThrows(IllegalArgumentException.class, () -> EstandaresIso.idioma("zz9"));
        assertThrows(IllegalArgumentException.class, () -> EstandaresIso.moneda("DOLARES"));
    }
}
