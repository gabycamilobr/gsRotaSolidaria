package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.domain.*;
import br.com.techalimenta.rotasolidaria.exception.NegocioException;
import br.com.techalimenta.rotasolidaria.repository.LoteRepository;
import br.com.techalimenta.rotasolidaria.repository.ReciboRepository;
import br.com.techalimenta.rotasolidaria.repository.ReservaRepository;
import br.com.techalimenta.rotasolidaria.web.dto.ReciboResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/** Confirmação da retirada pelo comerciante (QR code ou código de 4 dígitos) e emissão do recibo. */
@Service
public class RetiradaService {

    private final ReservaRepository reservas;
    private final LoteRepository lotes;
    private final ReciboRepository recibos;
    private final NotificacaoService notificacoes;
    private final Clock clock;

    public RetiradaService(ReservaRepository reservas, LoteRepository lotes, ReciboRepository recibos,
                           NotificacaoService notificacoes, Clock clock) {
        this.reservas = reservas;
        this.lotes = lotes;
        this.recibos = recibos;
        this.notificacoes = notificacoes;
        this.clock = clock;
    }

    @Transactional
    public ReciboResponse confirmar(Long comercioId, String codigo, Long reservaId) {
        List<Reserva> encontradas = reservas.findAtivasDoComercioPorCodigo(comercioId, codigo).stream()
                .filter(r -> reservaId == null || r.getId().equals(reservaId))
                .toList();
        if (encontradas.isEmpty()) {
            throw NegocioException.naoEncontrado("Código não encontrado entre as reservas ativas do seu comércio.");
        }
        Reserva reserva = encontradas.get(0);
        Instant agora = Instant.now(clock);
        if (reserva.getExpiraEm().isBefore(agora)) {
            throw NegocioException.conflito("Esta reserva expirou. O lote pode ser reservado de novo pelo app.");
        }

        Lote lote = lotes.findByIdParaAtualizar(reserva.getLote().getId()).orElseThrow();
        if (lote.getStatus() != StatusLote.RESERVADO) {
            throw NegocioException.conflito("Este lote não está mais reservado.");
        }
        lote.setStatus(StatusLote.RETIRADO);
        lote.setRetiradoEm(agora);
        lotes.save(lote);

        reserva.setStatus(StatusReserva.CONCLUIDA);
        reserva.setEncerradaEm(agora);
        reservas.save(reserva);

        Recibo recibo = gerarRecibo(lote, reserva.getReceptor(), agora);
        recibos.save(recibo);

        notificacoes.avisarReceptor(reserva.getReceptor().getId(), "Retirada confirmada",
                Textos.quantidade(lote) + " retirados em " + lote.getComercio().getNome() + ". Recibo " + recibo.getNumero() + ".",
                lote.getId());
        return ReciboResponse.de(recibo);
    }

    static Recibo gerarRecibo(Lote lote, Receptor receptor, Instant agora) {
        Recibo r = new Recibo();
        r.setLote(lote);
        r.setNumero(String.format("RS-%d-%06d", agora.atZone(ZoneOffset.UTC).getYear(), lote.getId()));
        r.setEmitidoEm(agora);
        r.setComercioNome(lote.getComercio().getNome());
        r.setComercioEndereco(lote.getComercio().getEndereco());
        r.setItem(lote.getDescricao());
        r.setCategoria(lote.getCategoria());
        r.setQuantidade(lote.getQuantidade());
        r.setUnidade(lote.getUnidade());
        r.setPesoKg(lote.getPesoKg());
        r.setReceptorNome(receptor.getNome());
        r.setReceptorEndereco(receptor.getEndereco());
        r.setConfirmacaoValidade(Recibo.TEXTO_CONFIRMACAO);
        r.setConfirmadoEm(lote.getConfirmadoEm());
        r.setTextoLegal(Recibo.TEXTO_LEGAL);
        return r;
    }
}
