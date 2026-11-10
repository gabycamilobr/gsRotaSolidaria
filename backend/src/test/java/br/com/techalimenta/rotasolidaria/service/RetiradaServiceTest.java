package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.domain.*;
import br.com.techalimenta.rotasolidaria.exception.NegocioException;
import br.com.techalimenta.rotasolidaria.repository.LoteRepository;
import br.com.techalimenta.rotasolidaria.repository.ReciboRepository;
import br.com.techalimenta.rotasolidaria.repository.ReservaRepository;
import br.com.techalimenta.rotasolidaria.web.dto.ReciboResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetiradaServiceTest {

    @Mock ReservaRepository reservas;
    @Mock LoteRepository lotes;
    @Mock ReciboRepository recibos;
    @Mock NotificacaoService notificacoes;

    RetiradaService service;
    Lote lote;
    Reserva reserva;

    @BeforeEach
    void setUp() {
        service = new RetiradaService(reservas, lotes, recibos, notificacoes, Clock.fixed(Fixtures.AGORA, ZoneOffset.UTC));
        lote = Fixtures.paes(StatusLote.RESERVADO);
        reserva = new Reserva();
        reserva.setId(5L);
        reserva.setLote(lote);
        reserva.setReceptor(Fixtures.maosDadas());
        reserva.setCodigo("4821");
        reserva.setStatus(StatusReserva.ATIVA);
        reserva.setExpiraEm(Fixtures.AGORA.plusSeconds(600));
    }

    @Test
    @DisplayName("código correto: lote RETIRADO, reserva CONCLUIDA e recibo com o texto da Lei 14.016/2020")
    void confirmaRetirada() {
        when(reservas.findAtivasDoComercioPorCodigo(1L, "4821")).thenReturn(List.of(reserva));
        when(lotes.findByIdParaAtualizar(10L)).thenReturn(Optional.of(lote));

        ReciboResponse recibo = service.confirmar(1L, "4821", null);

        assertThat(lote.getStatus()).isEqualTo(StatusLote.RETIRADO);
        assertThat(lote.getRetiradoEm()).isEqualTo(Fixtures.AGORA);
        assertThat(reserva.getStatus()).isEqualTo(StatusReserva.CONCLUIDA);
        assertThat(recibo.numero()).isEqualTo("RS-2026-000010");
        assertThat(recibo.textoLegal()).isEqualTo("Doação realizada de boa-fé, nos termos da Lei nº 14.016/2020.");
        assertThat(recibo.confirmacaoValidade()).contains("dentro da validade e foi conservado corretamente");
        assertThat(recibo.receptorNome()).isEqualTo("Associação Mãos Dadas");
        assertThat(recibo.pesoKg()).isEqualByComparingTo("1.0");
        assertThat(recibo.textoLegal() + recibo.confirmacaoValidade()).doesNotContainIgnoringCase("fiscal");
        verify(recibos).save(any(Recibo.class));
    }

    @Test
    @DisplayName("código inexistente no comércio devolve erro e não altera nada")
    void codigoInvalido() {
        when(reservas.findAtivasDoComercioPorCodigo(1L, "0000")).thenReturn(List.of());
        assertThatThrownBy(() -> service.confirmar(1L, "0000", null)).isInstanceOf(NegocioException.class);
        verifyNoInteractions(recibos);
    }

    @Test
    @DisplayName("reserva vencida (antes do agendador rodar) não pode ser retirada")
    void reservaVencida() {
        reserva.setExpiraEm(Fixtures.AGORA.minusSeconds(1));
        when(reservas.findAtivasDoComercioPorCodigo(1L, "4821")).thenReturn(List.of(reserva));
        assertThatThrownBy(() -> service.confirmar(1L, "4821", 5L)).hasMessageContaining("expirou");
        assertThat(lote.getStatus()).isEqualTo(StatusLote.RESERVADO);
    }
}
