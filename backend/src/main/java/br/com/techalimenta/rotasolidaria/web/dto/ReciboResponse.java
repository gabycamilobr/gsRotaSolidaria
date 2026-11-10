package br.com.techalimenta.rotasolidaria.web.dto;

import br.com.techalimenta.rotasolidaria.domain.Recibo;
import br.com.techalimenta.rotasolidaria.domain.Unidade;

import java.math.BigDecimal;
import java.time.Instant;

public record ReciboResponse(
        Long id,
        String numero,
        Long loteId,
        Instant emitidoEm,
        String comercioNome,
        String comercioEndereco,
        String item,
        String categoria,
        BigDecimal quantidade,
        Unidade unidade,
        BigDecimal pesoKg,
        String receptorNome,
        String receptorEndereco,
        String confirmacaoValidade,
        Instant confirmadoEm,
        String textoLegal) {

    public static ReciboResponse de(Recibo r) {
        return new ReciboResponse(r.getId(), r.getNumero(), r.getLote().getId(), r.getEmitidoEm(), r.getComercioNome(),
                r.getComercioEndereco(), r.getItem(), r.getCategoria().getRotulo(), r.getQuantidade(), r.getUnidade(),
                r.getPesoKg(), r.getReceptorNome(), r.getReceptorEndereco(), r.getConfirmacaoValidade(),
                r.getConfirmadoEm(), r.getTextoLegal());
    }
}
