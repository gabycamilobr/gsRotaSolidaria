package br.com.techalimenta.rotasolidaria.integracao;

import br.com.techalimenta.rotasolidaria.exception.NegocioException;
import br.com.techalimenta.rotasolidaria.service.ReservaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/** Duas organizações clicam em "Reservar" ao mesmo tempo: só uma leva o lote. */
class ReservaConcorrenciaPostgisTest extends PostgisTestBase {

    @Autowired ReservaService reservas;
    @Autowired JdbcTemplate jdbc;
    Long loteId;

    @AfterEach
    void limpar() {
        if (loteId == null) return;
        jdbc.update("DELETE FROM notificacao WHERE lote_id = ?", loteId);
        jdbc.update("DELETE FROM reserva WHERE lote_id = ?", loteId);
        jdbc.update("DELETE FROM lote WHERE id = ?", loteId);
    }

    @Test
    @DisplayName("reservas simultâneas: uma vence, a outra recebe conflito e só existe uma reserva ATIVA")
    void reservaSimultanea() throws Exception {
        loteId = jdbc.queryForObject("""
                INSERT INTO lote (comercio_id, categoria, descricao, quantidade, unidade, peso_kg, retirar_ate,
                                  confirmacao_validade, confirmado_em, latitude, longitude)
                VALUES (1, 'PANIFICADOS', 'pães', 20, 'UNIDADES', 1, now() + interval '2 hours', true, now(), -23.5891, -46.6347)
                RETURNING id""", Long.class);
        jdbc.update("INSERT INTO lote_candidato (lote_id, receptor_id, ordem, distancia_m) VALUES (?, 1, 1, 402), (?, 2, 2, 1184)",
                loteId, loteId);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<String>> resultados = new ArrayList<>();
        for (long receptor : new long[]{1L, 2L}) {
            resultados.add(pool.submit(() -> {
                largada.await();
                try {
                    reservas.reservar(receptor, loteId);
                    return "ok";
                } catch (NegocioException e) {
                    return "conflito";
                }
            }));
        }
        largada.countDown();
        List<String> r = new ArrayList<>();
        for (Future<String> f : resultados) r.add(f.get(10, TimeUnit.SECONDS));
        pool.shutdown();

        assertThat(r).containsExactlyInAnyOrder("ok", "conflito");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva WHERE lote_id = ? AND status = 'ATIVA'", Integer.class, loteId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM lote WHERE id = ?", String.class, loteId)).isEqualTo("RESERVADO");
    }
}
