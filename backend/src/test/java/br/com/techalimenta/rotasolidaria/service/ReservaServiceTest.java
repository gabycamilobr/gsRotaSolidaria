package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.domain.*;
import br.com.techalimenta.rotasolidaria.exception.NegocioException;
import br.com.techalimenta.rotasolidaria.repository.*;
import br.com.techalimenta.rotasolidaria.web.dto.ReservaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock LoteRepository lotes;
    @Mock ReservaRepository reservas;
    @Mock ReceptorRepository receptores;
    @Mock LoteCandidatoRepository candidatos;
    @Mock NotificacaoService notificacoes;

    ReservaService service;
    Receptor maosDadas = Fixtures.maosDadas();
    Receptor esperanca = Fixtures.esperanca();

    @BeforeEach
    void setUp() {
        service = new ReservaService(lotes, reservas, receptores, candidatos, notificacoes,
                Fixtures.props(45), Clock.fixed(Fixtures.AGORA, ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("Reserva")
    class Reservar {

        @Test
        @DisplayName("primeiro receptor leva o lote: RESERVADO, código de 4 dígitos e validade de 45 min")
        void reservaComSucesso() {
            Lote lote = Fixtures.paes(StatusLote.DISPONIVEL);
            when(lotes.findByIdParaAtualizar(10L)).thenReturn(Optional.of(lote));
            when(candidatos.findByLoteIdAndReceptorId(10L, 1L)).thenReturn(Optional.of(Fixtures.candidato(10L, maosDadas, 1, 402)));
            when(receptores.findById(1L)).thenReturn(Optional.of(maosDadas));
            when(reservas.findCodigosAtivosDoComercio(1L)).thenReturn(Set.of());

            ReservaResponse r = service.reservar(1L, 10L);

            assertThat(lote.getStatus()).isEqualTo(StatusLote.RESERVADO);
            assertThat(r.status()).isEqualTo(StatusReserva.ATIVA);
            assertThat(r.codigo()).matches("\\d{4}");
            assertThat(r.expiraEm()).isEqualTo(Fixtures.AGORA.plus(Duration.ofMinutes(45)));
            assertThat(r.lote().distanciaM()).isEqualTo(402);
            verify(reservas).save(any(Reserva.class));
            verify(notificacoes).avisarComercio(eq(1L), eq("Lote reservado"), contains("Associação Mãos Dadas"), eq(10L));
        }

        @Test
        @DisplayName("bloqueia reserva dupla: lote já RESERVADO devolve 409 e nada é gravado")
        void bloqueiaReservaDupla() {
            Lote lote = Fixtures.paes(StatusLote.RESERVADO);
            when(lotes.findByIdParaAtualizar(10L)).thenReturn(Optional.of(lote));
            when(candidatos.findByLoteIdAndReceptorId(10L, 2L)).thenReturn(Optional.of(Fixtures.candidato(10L, esperanca, 2, 1184)));

            assertThatThrownBy(() -> service.reservar(2L, 10L))
                    .isInstanceOf(NegocioException.class)
                    .satisfies(e -> assertThat(((NegocioException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT))
                    .hasMessageContaining("já foi reservado");
            verify(reservas, never()).save(any());
            verify(lotes, never()).save(any());
        }

        @Test
        @DisplayName("lê o lote com SELECT ... FOR UPDATE antes de decidir")
        void usaLockPessimista() {
            when(lotes.findByIdParaAtualizar(10L)).thenReturn(Optional.of(Fixtures.paes(StatusLote.RESERVADO)));
            when(candidatos.findByLoteIdAndReceptorId(10L, 1L)).thenReturn(Optional.of(Fixtures.candidato(10L, maosDadas, 1, 402)));
            assertThatThrownBy(() -> service.reservar(1L, 10L)).isInstanceOf(NegocioException.class);
            verify(lotes).findByIdParaAtualizar(10L);
            verify(lotes, never()).findById(any());
        }

        @Test
        @DisplayName("receptor fora da fila do matching não pode reservar")
        void foraDoRaio() {
            when(lotes.findByIdParaAtualizar(10L)).thenReturn(Optional.of(Fixtures.paes(StatusLote.DISPONIVEL)));
            when(candidatos.findByLoteIdAndReceptorId(10L, 3L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reservar(3L, 10L))
                    .isInstanceOf(NegocioException.class)
                    .satisfies(e -> assertThat(((NegocioException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("a reserva nunca vale além do 'retirar até' do lote")
        void reservaLimitadaAoPrazoDoLote() {
            Lote lote = Fixtures.paes(StatusLote.DISPONIVEL);
            lote.setRetirarAte(Fixtures.AGORA.plus(Duration.ofMinutes(20)));
            when(lotes.findByIdParaAtualizar(10L)).thenReturn(Optional.of(lote));
            when(candidatos.findByLoteIdAndReceptorId(10L, 1L)).thenReturn(Optional.of(Fixtures.candidato(10L, maosDadas, 1, 402)));
            when(receptores.findById(1L)).thenReturn(Optional.of(maosDadas));

            assertThat(service.reservar(1L, 10L).expiraEm()).isEqualTo(lote.getRetirarAte());
        }

        @Test
        @DisplayName("o código gerado não repete um código ativo do mesmo comércio")
        void codigoUnico() {
            Set<String> quaseTodos = new java.util.HashSet<>();
            for (int i = 0; i < 10_000; i++) quaseTodos.add(String.format("%04d", i));
            quaseTodos.remove("4321");
            assertThat(ReservaService.gerarCodigo(Set.of("1234"))).matches("\\d{4}").isNotEqualTo("1234");
            // com só um código livre a geração pode não encontrá-lo em 50 tentativas, mas nunca devolve um usado
            try {
                assertThat(ReservaService.gerarCodigo(quaseTodos)).isEqualTo("4321");
            } catch (NegocioException esperado) {
                assertThat(esperado.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            }
        }
    }

    @Nested
    @DisplayName("Expiração da reserva")
    class Expiracao {

        Reserva reservaVencida(Lote lote, Receptor receptor) {
            Reserva r = new Reserva();
            r.setId(99L);
            r.setLote(lote);
            r.setReceptor(receptor);
            r.setCodigo("1234");
            r.setStatus(StatusReserva.ATIVA);
            r.setCriadaEm(Fixtures.AGORA.minus(Duration.ofMinutes(46)));
            r.setExpiraEm(Fixtures.AGORA.minus(Duration.ofMinutes(1)));
            return r;
        }

        @Test
        @DisplayName("devolve o lote a DISPONIVEL e avisa o próximo candidato da fila")
        void devolveEAvisaProximo() {
            Lote lote = Fixtures.paes(StatusLote.RESERVADO);
            Reserva reserva = reservaVencida(lote, maosDadas);
            when(reservas.findById(99L)).thenReturn(Optional.of(reserva));
            when(lotes.findByIdParaAtualizar(10L)).thenReturn(Optional.of(lote));
            when(reservas.findReceptoresQueDeixaramExpirar(10L)).thenReturn(Set.of(1L));
            when(candidatos.findByLoteIdOrdenado(10L)).thenReturn(List.of(
                    Fixtures.candidato(10L, maosDadas, 1, 402), Fixtures.candidato(10L, esperanca, 2, 1184)));

            service.expirarReserva(99L);

            assertThat(reserva.getStatus()).isEqualTo(StatusReserva.EXPIRADA);
            assertThat(reserva.getEncerradaEm()).isEqualTo(Fixtures.AGORA);
            assertThat(lote.getStatus()).isEqualTo(StatusLote.DISPONIVEL);
            verify(notificacoes).avisarReceptor(eq(2L), eq("Lote disponível de novo"), anyString(), eq(10L));
            verify(notificacoes, never()).avisarReceptor(eq(1L), eq("Lote disponível de novo"), anyString(), anyLong());
            verify(notificacoes).avisarReceptor(eq(1L), eq("Reserva expirada"), anyString(), eq(10L));
        }

        @Test
        @DisplayName("pula quem já deixou a reserva deste lote expirar antes")
        void pulaQuemJaDeixouExpirar() {
            Receptor terceiro = Fixtures.receptor(4L, "Igreja do Bairro", -23.6, -46.64);
            List<LoteCandidato> fila = List.of(Fixtures.candidato(10L, maosDadas, 1, 402),
                    Fixtures.candidato(10L, esperanca, 2, 1184), Fixtures.candidato(10L, terceiro, 3, 1500));

            assertThat(ReservaService.proximoCandidato(fila, Set.of(1L, 2L))).get()
                    .extracting(LoteCandidato::getReceptorId).isEqualTo(4L);
            assertThat(ReservaService.proximoCandidato(fila, Set.of(2L))).get()
                    .extracting(LoteCandidato::getReceptorId).isEqualTo(1L);
            assertThat(ReservaService.proximoCandidato(fila, Set.of(1L, 2L, 4L))).isEmpty();
        }

        @Test
        @DisplayName("se o 'retirar até' também passou, o lote vira EXPIRADO e ninguém é avisado")
        void lotePassouDoPrazo() {
            Lote lote = Fixtures.paes(StatusLote.RESERVADO);
            lote.setRetirarAte(Fixtures.AGORA.minusSeconds(1));
            Reserva reserva = reservaVencida(lote, maosDadas);
            when(reservas.findById(99L)).thenReturn(Optional.of(reserva));
            when(lotes.findByIdParaAtualizar(10L)).thenReturn(Optional.of(lote));

            service.expirarReserva(99L);

            assertThat(lote.getStatus()).isEqualTo(StatusLote.EXPIRADO);
            verify(candidatos, never()).findByLoteIdOrdenado(any());
            verify(notificacoes, never()).avisarReceptor(eq(2L), anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("reserva ainda dentro do prazo não é tocada")
        void reservaNoPrazo() {
            Lote lote = Fixtures.paes(StatusLote.RESERVADO);
            Reserva reserva = reservaVencida(lote, maosDadas);
            reserva.setExpiraEm(Fixtures.AGORA.plusSeconds(60));
            when(reservas.findById(99L)).thenReturn(Optional.of(reserva));

            service.expirarReserva(99L);

            assertThat(reserva.getStatus()).isEqualTo(StatusReserva.ATIVA);
            assertThat(lote.getStatus()).isEqualTo(StatusLote.RESERVADO);
            verifyNoInteractions(notificacoes);
        }
    }
}
