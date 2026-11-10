package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.config.AppProperties;
import br.com.techalimenta.rotasolidaria.domain.Lote;
import br.com.techalimenta.rotasolidaria.domain.LoteCandidato;
import br.com.techalimenta.rotasolidaria.repository.LoteCandidatoRepository;
import br.com.techalimenta.rotasolidaria.repository.LoteCandidatoRepository.ReceptorProximo;
import br.com.techalimenta.rotasolidaria.web.dto.CandidatoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Matching geográfico: ao publicar um lote, seleciona os receptores num raio de 2 km que aceitam
 * a categoria (ST_DWithin), na ordem de distância (ST_Distance), grava a fila em lote_candidato
 * e avisa cada um deles.
 */
@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final LoteCandidatoRepository candidatos;
    private final NotificacaoService notificacoes;
    private final AppProperties props;
    private final Clock clock;

    public MatchingService(LoteCandidatoRepository candidatos, NotificacaoService notificacoes,
                           AppProperties props, Clock clock) {
        this.candidatos = candidatos;
        this.notificacoes = notificacoes;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public List<CandidatoResponse> executar(Lote lote) {
        long inicio = System.nanoTime();
        List<ReceptorProximo> proximos = candidatos.buscarReceptoresProximos(lote.getId(), props.matching().raioMetros());
        log.info("Matching do lote {}: {} receptor(es) em {} ms", lote.getId(), proximos.size(),
                (System.nanoTime() - inicio) / 1_000_000);

        Instant agora = Instant.now(clock);
        List<CandidatoResponse> resposta = new ArrayList<>();
        int ordem = 1;
        for (ReceptorProximo r : proximos) {
            int distancia = r.getDistanciaM().intValue();
            LoteCandidato c = new LoteCandidato(lote.getId(), r.getReceptorId(), ordem, distancia);
            c.setNotificadoEm(agora);
            candidatos.save(c);
            notificacoes.avisarReceptor(r.getReceptorId(), "Novo lote perto de você",
                    String.format("%s: %s a %s. Reserve no app.", lote.getComercio().getNome(),
                            Textos.quantidade(lote), Textos.distancia(distancia)),
                    lote.getId());
            resposta.add(new CandidatoResponse(ordem, r.getReceptorId(), r.getNome(), distancia));
            ordem++;
        }
        return resposta;
    }
}
