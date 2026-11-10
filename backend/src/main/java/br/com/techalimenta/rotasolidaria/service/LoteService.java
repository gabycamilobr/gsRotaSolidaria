package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.config.AppProperties;
import br.com.techalimenta.rotasolidaria.domain.*;
import br.com.techalimenta.rotasolidaria.exception.NegocioException;
import br.com.techalimenta.rotasolidaria.repository.*;
import br.com.techalimenta.rotasolidaria.security.UsuarioLogado;
import br.com.techalimenta.rotasolidaria.web.dto.CandidatoResponse;
import br.com.techalimenta.rotasolidaria.web.dto.LoteRequest;
import br.com.techalimenta.rotasolidaria.web.dto.LoteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LoteService {

    static final Duration JANELA_MAXIMA_REFRIGERADOS = Duration.ofHours(2);
    static final Duration JANELA_MAXIMA = Duration.ofHours(24);
    static final long FOTO_MAX_BYTES = 2 * 1024 * 1024;
    private static final Set<String> TIPOS_FOTO = Set.of("image/jpeg", "image/png", "image/webp");

    private final LoteRepository lotes;
    private final LoteFotoRepository fotos;
    private final ComercioRepository comercios;
    private final LoteCandidatoRepository candidatos;
    private final ReservaRepository reservas;
    private final MatchingService matching;
    private final NotificacaoService notificacoes;
    private final AppProperties props;
    private final Clock clock;

    public LoteService(LoteRepository lotes, LoteFotoRepository fotos, ComercioRepository comercios,
                       LoteCandidatoRepository candidatos, ReservaRepository reservas, MatchingService matching,
                       NotificacaoService notificacoes, AppProperties props, Clock clock) {
        this.lotes = lotes;
        this.fotos = fotos;
        this.comercios = comercios;
        this.candidatos = candidatos;
        this.reservas = reservas;
        this.matching = matching;
        this.notificacoes = notificacoes;
        this.props = props;
        this.clock = clock;
    }

    /** Publica um lote, guarda a foto, executa o matching e devolve os receptores avisados. */
    @Transactional
    public LoteResponse publicar(Long comercioId, LoteRequest req, MultipartFile foto) {
        Instant agora = Instant.now(clock);
        validar(req, foto, agora);

        Comercio comercio = comercios.findById(comercioId)
                .orElseThrow(() -> NegocioException.naoEncontrado("Comércio não encontrado."));

        Lote lote = new Lote();
        lote.setComercio(comercio);
        lote.setCategoria(req.categoria());
        lote.setDescricao(req.descricao().trim());
        lote.setQuantidade(req.quantidade());
        lote.setUnidade(req.unidade());
        lote.setPesoKg(req.pesoKg());
        lote.setRetirarAte(req.retirarAte());
        lote.setConfirmacaoValidade(true);
        lote.setConfirmadoEm(agora);
        lote.setStatus(StatusLote.DISPONIVEL);
        lote.setLatitude(comercio.getLatitude());
        lote.setLongitude(comercio.getLongitude());
        lote.setPublicadoEm(agora);
        lotes.saveAndFlush(lote);

        try {
            fotos.save(new LoteFoto(lote.getId(), foto.getBytes(), foto.getContentType()));
        } catch (IOException e) {
            throw NegocioException.regra("Não foi possível ler a foto. Tente de novo.");
        }

        List<CandidatoResponse> avisados = matching.executar(lote);
        return LoteResponse.de(lote, null, null, avisados);
    }

    void validar(LoteRequest req, MultipartFile foto, Instant agora) {
        if (!req.categoria().isAceita()) {
            throw NegocioException.regra(req.categoria().getRotulo() + ": " + req.categoria().getMotivoRecusa());
        }
        if (!req.confirmacaoValidade()) {
            throw NegocioException.regra("Confirme que o alimento está dentro da validade e foi conservado corretamente.");
        }
        if (!req.retirarAte().isAfter(agora)) {
            throw NegocioException.regra("O horário de retirada já passou.");
        }
        if (req.categoria().isRefrigerado() && req.retirarAte().isAfter(agora.plus(JANELA_MAXIMA_REFRIGERADOS))) {
            throw NegocioException.regra("Refrigerados precisam ser retirados em no máximo 2 horas.");
        }
        if (req.retirarAte().isAfter(agora.plus(JANELA_MAXIMA))) {
            throw NegocioException.regra("A retirada precisa acontecer nas próximas 24 horas.");
        }
        if (foto == null || foto.isEmpty()) {
            throw NegocioException.regra("Tire ou escolha uma foto do lote.");
        }
        if (foto.getSize() > FOTO_MAX_BYTES) {
            throw NegocioException.regra("A foto é grande demais (máximo 2 MB).");
        }
        if (foto.getContentType() == null || !TIPOS_FOTO.contains(foto.getContentType())) {
            throw NegocioException.regra("Envie a foto em JPEG, PNG ou WebP.");
        }
    }

    /** Lotes publicados hoje (fuso de São Paulo) pelo comércio, com a reserva ativa e a fila de candidatos. */
    @Transactional(readOnly = true)
    public List<LoteResponse> doComercioHoje(Long comercioId) {
        ZoneId fuso = ZoneId.of(props.fuso());
        Instant inicioDoDia = LocalDate.now(clock.withZone(fuso)).atStartOfDay(fuso).toInstant();
        List<Lote> lista = lotes.findDoComercioDesde(comercioId, inicioDoDia);
        return comReservas(lista, true);
    }

    @Transactional(readOnly = true)
    public List<LoteResponse> proximosDoReceptor(Long receptorId) {
        List<Lote> lista = lotes.findDisponiveisParaReceptor(receptorId, List.of(StatusLote.DISPONIVEL));
        if (lista.isEmpty()) return List.of();
        Map<Long, Integer> distancias = candidatos
                .findByReceptorIdAndLoteIdIn(receptorId, lista.stream().map(Lote::getId).toList()).stream()
                .collect(Collectors.toMap(LoteCandidato::getLoteId, LoteCandidato::getDistanciaM));
        return lista.stream()
                .map(l -> LoteResponse.de(l, distancias.get(l.getId()), null, null))
                .sorted(Comparator.comparing(r -> r.distanciaM() == null ? Integer.MAX_VALUE : r.distanciaM()))
                .toList();
    }

    @Transactional(readOnly = true)
    public LoteResponse detalhe(Long loteId, UsuarioLogado usuario) {
        Lote lote = lotes.findById(loteId).orElseThrow(() -> NegocioException.naoEncontrado("Lote não encontrado."));
        Reserva ativa = reservas.findFirstByLoteIdAndStatus(loteId, StatusReserva.ATIVA).orElse(null);
        switch (usuario.perfil()) {
            case COMERCIANTE -> {
                if (!lote.getComercio().getId().equals(usuario.comercioId())) {
                    throw NegocioException.proibido("Este lote é de outro comércio.");
                }
                return comReservas(List.of(lote), true).get(0);
            }
            case RECEPTOR -> {
                LoteCandidato c = candidatos.findByLoteIdAndReceptorId(loteId, usuario.receptorId())
                        .orElseThrow(() -> NegocioException.proibido("Este lote está fora do seu raio de atendimento."));
                boolean minha = ativa != null && ativa.getReceptor().getId().equals(usuario.receptorId());
                return LoteResponse.de(lote, c.getDistanciaM(), minha ? ativa : null, null);
            }
            default -> {
                return LoteResponse.de(lote);
            }
        }
    }

    @Transactional
    public LoteResponse cancelar(Long comercioId, Long loteId) {
        Lote lote = lotes.findByIdParaAtualizar(loteId)
                .orElseThrow(() -> NegocioException.naoEncontrado("Lote não encontrado."));
        if (!lote.getComercio().getId().equals(comercioId)) {
            throw NegocioException.proibido("Este lote é de outro comércio.");
        }
        if (lote.getStatus() != StatusLote.DISPONIVEL && lote.getStatus() != StatusLote.RESERVADO) {
            throw NegocioException.conflito("Só é possível cancelar lotes disponíveis ou reservados.");
        }
        Instant agora = Instant.now(clock);
        reservas.findFirstByLoteIdAndStatus(loteId, StatusReserva.ATIVA).ifPresent(r -> {
            r.setStatus(StatusReserva.CANCELADA);
            r.setEncerradaEm(agora);
            notificacoes.avisarReceptor(r.getReceptor().getId(), "Lote cancelado",
                    lote.getComercio().getNome() + " cancelou o lote de " + Textos.quantidade(lote) + ".", loteId);
        });
        lote.setStatus(StatusLote.CANCELADO);
        return LoteResponse.de(lote);
    }

    @Transactional(readOnly = true)
    public LoteFoto foto(Long loteId) {
        return fotos.findById(loteId).orElseThrow(() -> NegocioException.naoEncontrado("Foto não encontrada."));
    }

    private List<LoteResponse> comReservas(List<Lote> lista, boolean incluirCandidatos) {
        if (lista.isEmpty()) return List.of();
        List<Long> ids = lista.stream().map(Lote::getId).toList();
        Map<Long, Reserva> ativas = reservas.findByLoteIdInAndStatus(ids, StatusReserva.ATIVA).stream()
                .collect(Collectors.toMap(r -> r.getLote().getId(), Function.identity(), (a, b) -> a));
        List<LoteResponse> resposta = new ArrayList<>();
        for (Lote l : lista) {
            List<CandidatoResponse> fila = null;
            if (incluirCandidatos) {
                fila = candidatos.findByLoteIdOrdenado(l.getId()).stream()
                        .map(c -> new CandidatoResponse(c.getOrdem(), c.getReceptorId(), c.getReceptor().getNome(), c.getDistanciaM()))
                        .toList();
            }
            resposta.add(LoteResponse.de(l, null, ativas.get(l.getId()), fila));
        }
        return resposta;
    }
}
