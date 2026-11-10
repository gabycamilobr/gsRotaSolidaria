package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.config.AppProperties;
import br.com.techalimenta.rotasolidaria.domain.Comercio;
import br.com.techalimenta.rotasolidaria.domain.Lote;
import br.com.techalimenta.rotasolidaria.domain.StatusLote;
import br.com.techalimenta.rotasolidaria.exception.NegocioException;
import br.com.techalimenta.rotasolidaria.repository.ComercioRepository;
import br.com.techalimenta.rotasolidaria.repository.LoteRepository;
import br.com.techalimenta.rotasolidaria.repository.ReceptorRepository;
import br.com.techalimenta.rotasolidaria.web.dto.CartazResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Cartaz do dia (A4 imprimível) para quem não tem telefone. */
@Service
public class CartazService {

    private static final Set<StatusLote> VISIVEIS = Set.of(StatusLote.DISPONIVEL, StatusLote.RESERVADO, StatusLote.RETIRADO);

    private final ComercioRepository comercios;
    private final LoteRepository lotes;
    private final ReceptorRepository receptores;
    private final AppProperties props;
    private final Clock clock;

    public CartazService(ComercioRepository comercios, LoteRepository lotes, ReceptorRepository receptores,
                         AppProperties props, Clock clock) {
        this.comercios = comercios;
        this.lotes = lotes;
        this.receptores = receptores;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CartazResponse doDia(Long comercioId) {
        Comercio c = comercios.findById(comercioId)
                .orElseThrow(() -> NegocioException.naoEncontrado("Comércio não encontrado."));
        ZoneId fuso = ZoneId.of(props.fuso());
        LocalDate hoje = LocalDate.now(clock.withZone(fuso));
        List<Lote> doDia = lotes.findDoComercioDesde(comercioId, hoje.atStartOfDay(fuso).toInstant()).stream()
                .filter(l -> VISIVEIS.contains(l.getStatus()))
                .sorted(Comparator.comparing(Lote::getPublicadoEm))
                .toList();
        List<CartazResponse.Item> itens = doDia.stream().map(l -> new CartazResponse.Item(l.getDescricao(),
                l.getQuantidade(), l.getUnidade().name(), l.getCategoria().getRotulo(), l.getRetirarAte())).toList();
        Instant ate = doDia.stream().map(Lote::getRetirarAte).max(Comparator.naturalOrder()).orElse(null);
        CartazResponse.PontoDistribuicao ponto = receptores.maisProximoDoComercio(comercioId)
                .map(r -> new CartazResponse.PontoDistribuicao(r.getNome(), r.getEndereco(), r.getDistanciaM().intValue()))
                .orElse(null);
        return new CartazResponse(c.getNome(), c.getEndereco(), hoje, itens, ate, ponto);
    }
}
