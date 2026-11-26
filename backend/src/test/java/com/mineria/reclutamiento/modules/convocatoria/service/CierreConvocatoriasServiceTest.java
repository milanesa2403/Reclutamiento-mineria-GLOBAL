package com.mineria.reclutamiento.modules.convocatoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mineria.reclutamiento.modules.auth.entity.Rol;
import com.mineria.reclutamiento.modules.auth.entity.Usuario;
import com.mineria.reclutamiento.modules.convocatoria.entity.Convocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.EstadoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.entity.TipoConvocatoria;
import com.mineria.reclutamiento.modules.convocatoria.repository.ConvocatoriaRepository;
import com.mineria.reclutamiento.modules.empresa.entity.Empresa;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class CierreConvocatoriasServiceTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 14);

    @Test
    void cierraLasPublicadasConFechaDeCierreVencida() {
        ConvocatoriaRepository repo = mock(ConvocatoriaRepository.class);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        Usuario usuario = new Usuario("empresa@prueba.pe", Rol.EMPRESA);
        Convocatoria vencida = new Convocatoria(mock(Empresa.class), usuario, TipoConvocatoria.PLANILLA_COMPLETA);
        vencida.publicar(usuario, LocalDateTime.of(2026, 10, 1, 9, 0));
        when(repo.findByEstadoAndFechaCierreBefore(EstadoConvocatoria.PUBLICADA, HOY)).thenReturn(List.of(vencida));

        int cerradas = new CierreConvocatoriasService(repo, Clock.systemDefaultZone(), tx, true).cerrarVencidas(HOY);

        assertThat(cerradas).isEqualTo(1);
        assertThat(vencida.getEstado()).isEqualTo(EstadoConvocatoria.CERRADA);
    }
}
