package br.com.techalimenta.rotasolidaria.web.dto;

import br.com.techalimenta.rotasolidaria.domain.Notificacao;

import java.time.Instant;

public record NotificacaoResponse(Long id, String titulo, String mensagem, Long loteId, Instant criadaEm, boolean lida) {
    public static NotificacaoResponse de(Notificacao n) {
        return new NotificacaoResponse(n.getId(), n.getTitulo(), n.getMensagem(), n.getLoteId(), n.getCriadaEm(), n.isLida());
    }
}
