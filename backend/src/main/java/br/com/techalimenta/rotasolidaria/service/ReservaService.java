package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.config.AppProperties;
import br.com.techalimenta.rotasolidaria.domain.*;
import br.com.techalimenta.rotasolidaria.exception.NegocioException;
import br.com.techalimenta.rotasolidaria.repository.*;
import br.com.techalimenta.rotasolidaria.web.dto.ReservaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reserva de lotes. O primeiro receptor que reservar leva o lote:
 * o lote é lido com SELECT ... FOR UPDATE (lock pessimista) e ainda tem @Version;
 * no banco, um índice único parcial impede duas reservas ATIVAS para o mesmo lote.
 */
@Service
public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LoteRepository lotes;
    private final ReservaRepository reservas;
    private final ReceptorRepository receptores;
    private final LoteCandidatoRepository candidatos;
    private final NotificacaoService notificacoes;
    private final AppProperties props;
    private final Clock clock;

    public ReservaService(LoteRepository lotes, ReservaRepository reservas, ReceptorRepository receptores,
                          LoteCandidatoRepository candidatos, NotificacaoService notificacoes,
                          AppProperties props, Clock clock) {
        this.lotes = lotes;
        this.reservas = reservas;
        this.receptores = receptores;
        this.candidatos = candidatos;
        this.notificacoes = notificacoes;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public ReservaResponse reservar(Long receptorId, Long loteId) {
        Lote lote = lotes.findByIdParaAtualizar(loteId)
                .orElseThrow(() -> NegocioException.naoEncontrado("Lote não encontrado."));
        LoteCandidato candidato = candidatos.findByLoteIdAndReceptorId(loteId, receptorId)
                .orElseThrow(() -> NegocioException.proibido("Este lote está fora do seu raio de atendimento."));
        if (lote.getStatus() != StatusLote.DISPONIVEL) {
            throw NegocioException.conflito("Este lote já foi reservado por outra organização ou não está mais disponível.");
        }
        Instant agora = Instant.now(clock);
        if (!lote.getRetirarAte().isAfter(agora)) {
            throw NegocioException.conflito("O prazo de retirada deste lote já passou.");
        }

        Receptor receptor = receptores.findById(receptorId)
                .orElseThrow(() -> NegocioException.naoEncontrado("Receptor não encontrado."));

        Instant expira = agora.plus(Duration.ofMinutes(props.reserva().minutos()));
        if (expira.isAfter(lote.getRetirarAte())) {
            expira = lote.getRetirarAte();
        }

        Reserva reserva = new Reserva();
        reserva.setLote(lote);
        reserva.setReceptor(receptor);
        reserva.setCodigo(gerarCodigo(reservas.findCodigosAtivosDoComercio(lote.getComercio().getId())));
        reserva.setStatus(StatusReserva.ATIVA);
        reserva.setCriadaEm(agora);
        reserva.setExpiraEm(expira);
        reservas.save(reserva);

        lote.setStatus(StatusLote.RESERVADO);
        lotes.save(lote);

        notificacoes.avisarComercio(lote.getComercio().getId(), "Lote reservado",
                receptor.getNome() + " reservou " + Textos.quantidade(lote) + ". Peça o QR code ou o código na retirada.",
                lote.getId());
        notificacoes.avisarVoluntariosDoReceptor(receptorId, "Nova coleta na sua rota",
                "Buscar " + Textos.quantidade(lote) + " em " + lote.getComercio().getNome() + ".", lote.getId());

        return ReservaResponse.de(reserva, candidato.getDistanciaM());
    }

    /** Código de 4 dígitos, único entre as reservas ativas do mesmo comércio. */
    static String gerarCodigo(Set<String> emUso) {
        for (int i = 0; i < 50; i++) {
            String codigo = String.format("%04d", RANDOM.nextInt(10_000));
            if (!emUso.contains(codigo)) return codigo;
        }
        throw NegocioException.conflito("Não foi possível gerar um código de retirada. Tente de novo.");
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> doReceptor(Long receptorId) {
        List<Reserva> lista = reservas.findDoReceptor(receptorId);
        if (lista.isEmpty()) return List.of();
        Map<Long, Integer> distancias = candidatos
                .findByReceptorIdAndLoteIdIn(receptorId, lista.stream().map(r -> r.getLote().getId()).distinct().toList())
                .stream().collect(Collectors.toMap(LoteCandidato::getLoteId, LoteCandidato::getDistanciaM));
        return lista.stream().map(r -> ReservaResponse.de(r, distancias.get(r.getLote().getId()))).toList();
    }

    /**
     * Expira uma reserva vencida: o lote volta a DISPONIVEL e o próximo candidato da fila
     * (excluindo quem já deixou a reserva deste lote expirar) é avisado.
     */
    @Transactional
    public void expirarReserva(Long reservaId) {
        Reserva reserva = reservas.findById(reservaId).orElse(null);
        if (reserva == null || reserva.getStatus() != StatusReserva.ATIVA) return;
        Instant agora = Instant.now(clock);
        if (reserva.getExpiraEm().isAfter(agora)) return;

        Lote lote = lotes.findByIdParaAtualizar(reserva.getLote().getId()).orElseThrow();
        reserva.setStatus(StatusReserva.EXPIRADA);
        reserva.setEncerradaEm(agora);
        reservas.save(reserva);

        Long receptorQueExpirou = reserva.getReceptor().getId();
        notificacoes.avisarReceptor(receptorQueExpirou, "Reserva expirada",
                "Sua reserva de " + Textos.quantidade(lote) + " expirou e o lote foi liberado.", lote.getId());

        if (lote.getStatus() != StatusLote.RESERVADO) return;
        if (!lote.getRetirarAte().isAfter(agora)) {
            lote.setStatus(StatusLote.EXPIRADO);
            lotes.save(lote);
            return;
        }
        lote.setStatus(StatusLote.DISPONIVEL);
        lotes.save(lote);
        log.info("Reserva {} expirou; lote {} voltou a DISPONIVEL", reservaId, lote.getId());

        Set<Long> excluidos = new HashSet<>(reservas.findReceptoresQueDeixaramExpirar(lote.getId()));
        excluidos.add(receptorQueExpirou);
        proximoCandidato(candidatos.findByLoteIdOrdenado(lote.getId()), excluidos).ifPresent(proximo -> {
            proximo.setNotificadoEm(agora);
            candidatos.save(proximo);
            notificacoes.avisarReceptor(proximo.getReceptorId(), "Lote disponível de novo",
                    lote.getComercio().getNome() + ": " + Textos.quantidade(lote) + " a "
                            + Textos.distancia(proximo.getDistanciaM()) + ". Reserve no app.", lote.getId());
            notificacoes.avisarComercio(lote.getComercio().getId(), "Reserva expirou",
                    "O lote de " + Textos.quantidade(lote) + " voltou a ficar disponível e "
                            + proximo.getReceptor().getNome() + " foi avisada.", lote.getId());
        });
    }

    /** Primeiro da fila (em ordem de distância) que não deixou a reserva deste lote expirar. */
    static Optional<LoteCandidato> proximoCandidato(List<LoteCandidato> filaOrdenada, Set<Long> excluidos) {
        return filaOrdenada.stream()
                .sorted(Comparator.comparingInt(LoteCandidato::getOrdem))
                .filter(c -> !excluidos.contains(c.getReceptorId()))
                .findFirst();
    }

    /** Lote que passou do "retirar até" sem retirada vira EXPIRADO. */
    @Transactional
    public void expirarLote(Long loteId) {
        Lote lote = lotes.findByIdParaAtualizar(loteId).orElse(null);
        if (lote == null) return;
        Instant agora = Instant.now(clock);
        if (lote.getRetirarAte().isAfter(agora)) return;
        if (lote.getStatus() != StatusLote.DISPONIVEL && lote.getStatus() != StatusLote.RESERVADO) return;
        reservas.findFirstByLoteIdAndStatus(loteId, StatusReserva.ATIVA).ifPresent(r -> {
            r.setStatus(StatusReserva.EXPIRADA);
            r.setEncerradaEm(agora);
        });
        lote.setStatus(StatusLote.EXPIRADO);
        lotes.save(lote);
        log.info("Lote {} passou do horário de retirada: EXPIRADO", loteId);
    }
}
