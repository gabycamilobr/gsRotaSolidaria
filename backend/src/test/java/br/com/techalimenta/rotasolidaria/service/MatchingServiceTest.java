package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.domain.Lote;
import br.com.techalimenta.rotasolidaria.domain.LoteCandidato;
import br.com.techalimenta.rotasolidaria.domain.StatusLote;
import br.com.techalimenta.rotasolidaria.repository.LoteCandidatoRepository;
import br.com.techalimenta.rotasolidaria.repository.LoteCandidatoRepository.ReceptorProximo;
import br.com.techalimenta.rotasolidaria.web.dto.CandidatoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comportamento do serviço de matching (fila de candidatos e avisos).
 * O filtro geográfico e de categoria em si (ST_DWithin + ANY) é testado contra o PostGIS real em
 * {@code integracao.MatchingConsultaIT}.
 */
@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock LoteCandidatoRepository candidatos;
    @Mock NotificacaoService notificacoes;

    MatchingService service;
    Lote lote;

    @BeforeEach
    void setUp() {
        service = new MatchingService(candidatos, notificacoes, Fixtures.props(45), Clock.fixed(Fixtures.AGORA, ZoneOffset.UTC));
        lote = Fixtures.paes(StatusLote.DISPONIVEL);
    }

    static ReceptorProximo proximo(long id, String nome, int distancia) {
        return new ReceptorProximo() {
            public Long getReceptorId() { return id; }
            public String getNome() { return nome; }
            public Number getDistanciaM() { return (double) distancia; }
        };
    }

    @Test
    @DisplayName("consulta o raio de 2.000 m, grava a fila na ordem de distância e avisa cada receptor")
    void gravaFilaEAvisa() {
        when(candidatos.buscarReceptoresProximos(10L, 2000.0)).thenReturn(List.of(
                proximo(1, "Associação Mãos Dadas", 402), proximo(2, "Comunidade Esperança", 1184)));

        List<CandidatoResponse> resposta = service.executar(lote);

        assertThat(resposta).extracting(CandidatoResponse::nome)
                .containsExactly("Associação Mãos Dadas", "Comunidade Esperança");
        ArgumentCaptor<LoteCandidato> fila = ArgumentCaptor.forClass(LoteCandidato.class);
        verify(candidatos, times(2)).save(fila.capture());
        assertThat(fila.getAllValues()).extracting(LoteCandidato::getReceptorId, LoteCandidato::getOrdem, LoteCandidato::getDistanciaM)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1L, 1, 402), org.assertj.core.groups.Tuple.tuple(2L, 2, 1184));
        verify(notificacoes).avisarReceptor(eq(1L), anyString(), contains("402 m"), eq(10L));
        verify(notificacoes).avisarReceptor(eq(2L), anyString(), contains("1,2 km"), eq(10L));
    }

    @Test
    @DisplayName("sem receptor no raio (ou que aceite a categoria), ninguém é gravado nem avisado")
    void semCandidatos() {
        when(candidatos.buscarReceptoresProximos(anyLong(), anyDouble())).thenReturn(List.of());

        assertThat(service.executar(lote)).isEmpty();
        verify(candidatos, never()).save(any());
        verifyNoInteractions(notificacoes);
    }
}
