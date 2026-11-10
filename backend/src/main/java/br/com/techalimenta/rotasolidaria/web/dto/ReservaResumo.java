package br.com.techalimenta.rotasolidaria.web.dto;

import br.com.techalimenta.rotasolidaria.domain.Reserva;
import br.com.techalimenta.rotasolidaria.domain.StatusReserva;

import java.time.Instant;

public record ReservaResumo(Long id, StatusReserva status, Long receptorId, String receptorNome, Instant expiraEm) {
    public static ReservaResumo de(Reserva r) {
        return new ReservaResumo(r.getId(), r.getStatus(), r.getReceptor().getId(), r.getReceptor().getNome(), r.getExpiraEm());
    }
}
