package br.com.techalimenta.rotasolidaria.web.dto;

import br.com.techalimenta.rotasolidaria.domain.Reserva;
import br.com.techalimenta.rotasolidaria.domain.StatusReserva;

import java.time.Instant;

public record ReservaResponse(
        Long id,
        StatusReserva status,
        String codigo,
        String qrConteudo,
        Instant criadaEm,
        Instant expiraEm,
        Instant encerradaEm,
        String receptorNome,
        String receptorEndereco,
        LoteResponse lote) {

    public static ReservaResponse de(Reserva r, Integer distanciaM) {
        return new ReservaResponse(r.getId(), r.getStatus(), r.getCodigo(), r.qrConteudo(), r.getCriadaEm(),
                r.getExpiraEm(), r.getEncerradaEm(), r.getReceptor().getNome(), r.getReceptor().getEndereco(),
                LoteResponse.de(r.getLote(), distanciaM, null, null));
    }
}
