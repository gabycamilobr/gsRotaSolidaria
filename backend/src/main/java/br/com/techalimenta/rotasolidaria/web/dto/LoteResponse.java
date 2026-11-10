package br.com.techalimenta.rotasolidaria.web.dto;

import br.com.techalimenta.rotasolidaria.domain.*;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoteResponse(
        Long id,
        Categoria categoria,
        String categoriaRotulo,
        boolean refrigerado,
        String descricao,
        BigDecimal quantidade,
        Unidade unidade,
        BigDecimal pesoKg,
        Instant retirarAte,
        StatusLote status,
        Instant publicadoEm,
        Instant retiradoEm,
        boolean confirmacaoValidade,
        String fotoUrl,
        ComercioResumo comercio,
        Integer distanciaM,
        ReservaResumo reserva,
        List<CandidatoResponse> candidatos) {

    public static LoteResponse de(Lote l, Integer distanciaM, Reserva reserva, List<CandidatoResponse> candidatos) {
        return new LoteResponse(l.getId(), l.getCategoria(), l.getCategoria().getRotulo(), l.getCategoria().isRefrigerado(),
                l.getDescricao(), l.getQuantidade(), l.getUnidade(), l.getPesoKg(), l.getRetirarAte(), l.getStatus(),
                l.getPublicadoEm(), l.getRetiradoEm(), l.isConfirmacaoValidade(),
                "/api/lotes/" + l.getId() + "/foto", ComercioResumo.de(l.getComercio()), distanciaM,
                reserva == null ? null : ReservaResumo.de(reserva), candidatos);
    }

    public static LoteResponse de(Lote l) {
        return de(l, null, null, null);
    }
}
