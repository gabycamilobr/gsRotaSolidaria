package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.config.AppProperties;
import br.com.techalimenta.rotasolidaria.domain.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Dados de teste espelhando o seed de demonstração. */
final class Fixtures {

    static final Instant AGORA = Instant.parse("2026-11-10T22:00:00Z"); // 19h em São Paulo

    private Fixtures() {}

    static AppProperties props(int minutosReserva) {
        return new AppProperties(new AppProperties.Reserva(minutosReserva), new AppProperties.Matching(2000),
                new AppProperties.Jwt("segredo-de-teste-com-mais-de-32-caracteres!!", 1),
                new AppProperties.Cors(List.of("http://localhost:5173")), new AppProperties.Firebase(""),
                "America/Sao_Paulo");
    }

    static Comercio padaria() {
        Comercio c = new Comercio();
        c.setId(1L);
        c.setNome("Padaria Bom Dia");
        c.setEndereco("Rua Domingos de Morais, 1500");
        c.setLatitude(-23.58910);
        c.setLongitude(-46.63470);
        return c;
    }

    static Comercio quitanda() {
        Comercio c = new Comercio();
        c.setId(2L);
        c.setNome("Quitanda da Vila");
        c.setEndereco("Rua Joaquim Távora, 520");
        c.setLatitude(-23.59050);
        c.setLongitude(-46.63750);
        return c;
    }

    static Receptor receptor(long id, String nome, double lat, double lng) {
        Receptor r = new Receptor();
        r.setId(id);
        r.setNome(nome);
        r.setEndereco("Endereço de " + nome);
        r.setLatitude(lat);
        r.setLongitude(lng);
        return r;
    }

    static Receptor maosDadas() {
        return receptor(1L, "Associação Mãos Dadas", -23.59270, -46.63520);
    }

    static Receptor esperanca() {
        return receptor(2L, "Comunidade Esperança", -23.59300, -46.64550);
    }

    static Lote lote(long id, Comercio comercio, Categoria categoria, String descricao, String qtd, Unidade unidade,
                     String pesoKg, StatusLote status) {
        Lote l = new Lote();
        l.setId(id);
        l.setComercio(comercio);
        l.setCategoria(categoria);
        l.setDescricao(descricao);
        l.setQuantidade(new BigDecimal(qtd));
        l.setUnidade(unidade);
        l.setPesoKg(new BigDecimal(pesoKg));
        l.setStatus(status);
        l.setLatitude(comercio.getLatitude());
        l.setLongitude(comercio.getLongitude());
        l.setPublicadoEm(AGORA.minusSeconds(600));
        l.setRetirarAte(AGORA.plusSeconds(2 * 3600));
        l.setConfirmacaoValidade(true);
        l.setConfirmadoEm(AGORA.minusSeconds(600));
        return l;
    }

    static Lote paes(StatusLote status) {
        return lote(10L, padaria(), Categoria.PANIFICADOS, "pães franceses", "20", Unidade.UNIDADES, "1.0", status);
    }

    static LoteCandidato candidato(long loteId, Receptor r, int ordem, int distancia) {
        LoteCandidato c = new LoteCandidato(loteId, r.getId(), ordem, distancia);
        c.setReceptor(r);
        return c;
    }
}
