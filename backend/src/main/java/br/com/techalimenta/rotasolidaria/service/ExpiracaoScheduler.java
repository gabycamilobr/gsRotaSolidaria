package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.domain.StatusLote;
import br.com.techalimenta.rotasolidaria.repository.LoteRepository;
import br.com.techalimenta.rotasolidaria.repository.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Verifica periodicamente reservas vencidas e lotes que passaram do horário de retirada. */
@Component
public class ExpiracaoScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiracaoScheduler.class);

    private final ReservaRepository reservas;
    private final LoteRepository lotes;
    private final ReservaService reservaService;
    private final Clock clock;

    public ExpiracaoScheduler(ReservaRepository reservas, LoteRepository lotes, ReservaService reservaService, Clock clock) {
        this.reservas = reservas;
        this.lotes = lotes;
        this.reservaService = reservaService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.agendador.intervalo-ms:15000}", initialDelay = 5000)
    public void verificar() {
        Instant agora = Instant.now(clock);
        for (Long id : reservas.findIdsVencidas(agora)) {
            try {
                reservaService.expirarReserva(id);
            } catch (Exception e) {
                log.warn("Falha ao expirar reserva {}: {}", id, e.getMessage());
            }
        }
        List<Long> vencidos = lotes.findIdsComPrazoVencido(List.of(StatusLote.DISPONIVEL, StatusLote.RESERVADO), agora);
        for (Long id : vencidos) {
            try {
                reservaService.expirarLote(id);
            } catch (Exception e) {
                log.warn("Falha ao expirar lote {}: {}", id, e.getMessage());
            }
        }
    }
}
