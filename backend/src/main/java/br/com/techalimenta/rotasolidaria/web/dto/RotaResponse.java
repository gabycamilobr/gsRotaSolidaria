package br.com.techalimenta.rotasolidaria.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RotaResponse(
        String voluntarioNome,
        String modal,
        String modalRotulo,
        int velocidadeKmH,
        Ponto origem,
        List<Parada> paradas,
        int distanciaTotalM,
        int tempoEstimadoMin,
        String googleMapsUrl) {

    public record Ponto(String nome, double latitude, double longitude) {}

    /** tipo: COLETA (comércio) ou ENTREGA (ponto de distribuição do receptor). */
    public record Parada(int ordem, String tipo, String nome, String endereco, double latitude, double longitude,
                         int distanciaTrechoM, List<Item> itens) {}

    public record Item(Long loteId, Long reservaId, String descricao, BigDecimal quantidade, String unidade,
                       BigDecimal pesoKg, boolean refrigerado, Instant retirarAte, Instant reservaExpiraEm,
                       String codigo, String qrConteudo) {}
}
