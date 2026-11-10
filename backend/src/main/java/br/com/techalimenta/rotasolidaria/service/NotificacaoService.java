package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.domain.Notificacao;
import br.com.techalimenta.rotasolidaria.domain.Usuario;
import br.com.techalimenta.rotasolidaria.repository.NotificacaoRepository;
import br.com.techalimenta.rotasolidaria.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Avisos aos usuários. Todo aviso é gravado (aparece dentro do app pelo polling)
 * e, se o usuário tiver token e o Firebase estiver ativo, também vai por push –
 * sempre depois do commit, para não segurar o lock do lote durante a chamada externa.
 */
@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacoes;
    private final UsuarioRepository usuarios;
    private final PushSender push;
    private final Clock clock;

    public NotificacaoService(NotificacaoRepository notificacoes, UsuarioRepository usuarios, PushSender push, Clock clock) {
        this.notificacoes = notificacoes;
        this.usuarios = usuarios;
        this.push = push;
        this.clock = clock;
    }

    @Transactional
    public void avisarReceptor(Long receptorId, String titulo, String mensagem, Long loteId) {
        avisar(usuarios.findByReceptorId(receptorId), titulo, mensagem, loteId);
    }

    @Transactional
    public void avisarComercio(Long comercioId, String titulo, String mensagem, Long loteId) {
        avisar(usuarios.findByComercioId(comercioId), titulo, mensagem, loteId);
    }

    @Transactional
    public void avisarVoluntariosDoReceptor(Long receptorId, String titulo, String mensagem, Long loteId) {
        avisar(usuarios.findVoluntariosDoReceptor(receptorId), titulo, mensagem, loteId);
    }

    private void avisar(List<Usuario> destinatarios, String titulo, String mensagem, Long loteId) {
        Instant agora = Instant.now(clock);
        for (Usuario u : destinatarios) {
            Notificacao n = new Notificacao();
            n.setUsuarioId(u.getId());
            n.setTitulo(titulo);
            n.setMensagem(mensagem);
            n.setLoteId(loteId);
            n.setCriadaEm(agora);
            notificacoes.save(n);
            if (u.getPushToken() != null) {
                String token = u.getPushToken();
                depoisDoCommit(() -> push.enviar(token, titulo, mensagem, loteId));
            }
        }
    }

    private void depoisDoCommit(Runnable acao) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    acao.run();
                }
            });
        } else {
            acao.run();
        }
    }
}
