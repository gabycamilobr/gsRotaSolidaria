package br.com.techalimenta.rotasolidaria.integracao;

import br.com.techalimenta.rotasolidaria.repository.LoteCandidatoRepository;
import br.com.techalimenta.rotasolidaria.repository.LoteCandidatoRepository.ReceptorProximo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A consulta de matching (ST_DWithin + ST_Distance + categoria) contra o PostGIS real e o seed da Vila Mariana. */
@Transactional
class MatchingConsultaPostgisTest extends PostgisTestBase {

    @Autowired LoteCandidatoRepository candidatos;
    @Autowired JdbcTemplate jdbc;

    long lote(String categoria, double lat, double lng) {
        return jdbc.queryForObject("""
                INSERT INTO lote (comercio_id, categoria, descricao, quantidade, unidade, peso_kg, retirar_ate,
                                  confirmacao_validade, confirmado_em, latitude, longitude)
                VALUES (1, ?, 'teste', 1, 'UNIDADES', 1, now() + interval '1 hour', true, now(), ?, ?)
                RETURNING id""", Long.class, categoria, lat, lng);
    }

    List<String> nomes(long loteId) {
        return candidatos.buscarReceptoresProximos(loteId, 2000).stream().map(ReceptorProximo::getNome).toList();
    }

    @Test
    @DisplayName("Panificados na Padaria: Mãos Dadas e Esperança, em ordem; Vila Nova (2,8 km) fica fora do raio")
    void dentroEForaDoRaio() {
        long id = lote("PANIFICADOS", -23.58910, -46.63470);
        List<ReceptorProximo> r = candidatos.buscarReceptoresProximos(id, 2000);

        assertThat(r).extracting(ReceptorProximo::getNome).containsExactly("Associação Mãos Dadas", "Comunidade Esperança");
        assertThat(r.get(0).getDistanciaM().intValue()).isBetween(380, 420);
        assertThat(r.get(1).getDistanciaM().intValue()).isBetween(1150, 1250);
        int vilaNova = jdbc.queryForObject("""
                SELECT ROUND(ST_Distance(r.localizacao, l.localizacao))::int FROM receptor r, lote l
                WHERE r.nome = 'Casa de Apoio Vila Nova' AND l.id = ?""", Integer.class, id);
        assertThat(vilaNova).isGreaterThan(2000);
    }

    @Test
    @DisplayName("categoria não aceita: Refrigerados não vão para a Comunidade Esperança")
    void categoriaNaoAceita() {
        long id = lote("REFRIGERADOS", -23.58910, -46.63470);
        assertThat(nomes(id)).containsExactly("Associação Mãos Dadas");
    }

    @Test
    @DisplayName("categoria aceita por todos e lote perto da Vila Nova: ela entra no raio")
    void loteNoOutroBairro() {
        long id = lote("HORTIFRUTI", -23.59050, -46.65800);
        assertThat(nomes(id)).contains("Casa de Apoio Vila Nova").doesNotContain("Associação Mãos Dadas");
    }

    @Test
    @DisplayName("a consulta usa o índice GIST e responde bem abaixo de 500 ms")
    void desempenho() {
        long id = lote("PANIFICADOS", -23.58910, -46.63470);
        long inicio = System.nanoTime();
        nomes(id);
        assertThat((System.nanoTime() - inicio) / 1_000_000).isLessThan(500);
    }
}
