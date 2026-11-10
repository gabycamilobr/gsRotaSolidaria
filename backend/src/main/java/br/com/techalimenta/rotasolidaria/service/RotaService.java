package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.domain.*;
import br.com.techalimenta.rotasolidaria.exception.NegocioException;
import br.com.techalimenta.rotasolidaria.repository.ReservaRepository;
import br.com.techalimenta.rotasolidaria.repository.VoluntarioRepository;
import br.com.techalimenta.rotasolidaria.web.dto.RotaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Rota de coleta do voluntário: parte da localização dele, passa pelos comércios com lotes
 * reservados pelo receptor ao qual está vinculado (vizinho mais próximo) e termina no
 * ponto de distribuição do receptor. Sem API paga: distâncias em linha reta e link de
 * direções do Google Maps montado por URL.
 */
@Service
public class RotaService {

    private final VoluntarioRepository voluntarios;
    private final ReservaRepository reservas;

    public RotaService(VoluntarioRepository voluntarios, ReservaRepository reservas) {
        this.voluntarios = voluntarios;
        this.reservas = reservas;
    }

    /** Ponto genérico da rota. */
    public record Ponto(String chave, double latitude, double longitude) {}

    @Transactional(readOnly = true)
    public RotaResponse calcular(Long voluntarioId, Double latAtual, Double lngAtual) {
        Voluntario v = voluntarios.findById(voluntarioId)
                .orElseThrow(() -> NegocioException.naoEncontrado("Voluntário não encontrado."));
        Receptor receptor = v.getReceptor();

        boolean usaAtual = latAtual != null && lngAtual != null;
        RotaResponse.Ponto origem = new RotaResponse.Ponto(usaAtual ? "Sua localização atual" : "Ponto de partida de " + v.getNome(),
                usaAtual ? latAtual : v.getLatitude(), usaAtual ? lngAtual : v.getLongitude());

        // agrupa os lotes reservados por comércio: cada comércio é uma parada de coleta
        Map<Long, List<Reserva>> porComercio = reservas.findAtivasDoReceptor(receptor.getId()).stream()
                .collect(Collectors.groupingBy(r -> r.getLote().getComercio().getId(), LinkedHashMap::new, Collectors.toList()));
        Map<String, Comercio> comercios = new HashMap<>();
        List<Ponto> coletas = new ArrayList<>();
        porComercio.forEach((id, lista) -> {
            Comercio c = lista.get(0).getLote().getComercio();
            comercios.put(id.toString(), c);
            coletas.add(new Ponto(id.toString(), c.getLatitude(), c.getLongitude()));
        });

        List<Ponto> ordem = vizinhoMaisProximo(new Ponto("origem", origem.latitude(), origem.longitude()), coletas);

        List<RotaResponse.Parada> paradas = new ArrayList<>();
        double latAnt = origem.latitude(), lngAnt = origem.longitude();
        int total = 0;
        int n = 1;
        for (Ponto p : ordem) {
            Comercio c = comercios.get(p.chave());
            int trecho = (int) Math.round(Geo.distanciaMetros(latAnt, lngAnt, p.latitude(), p.longitude()));
            total += trecho;
            List<RotaResponse.Item> itens = porComercio.get(c.getId()).stream().map(r -> new RotaResponse.Item(
                    r.getLote().getId(), r.getId(), r.getLote().getDescricao(), r.getLote().getQuantidade(),
                    r.getLote().getUnidade().name(), r.getLote().getPesoKg(), r.getLote().getCategoria().isRefrigerado(),
                    r.getLote().getRetirarAte(), r.getExpiraEm(), r.getCodigo(), r.qrConteudo())).toList();
            paradas.add(new RotaResponse.Parada(n++, "COLETA", c.getNome(), c.getEndereco(), c.getLatitude(), c.getLongitude(), trecho, itens));
            latAnt = p.latitude();
            lngAnt = p.longitude();
        }
        if (!paradas.isEmpty()) {
            int trecho = (int) Math.round(Geo.distanciaMetros(latAnt, lngAnt, receptor.getLatitude(), receptor.getLongitude()));
            total += trecho;
            paradas.add(new RotaResponse.Parada(n, "ENTREGA", receptor.getNome(), receptor.getEndereco(),
                    receptor.getLatitude(), receptor.getLongitude(), trecho, List.of()));
        }

        Modal modal = v.getModal();
        String url = paradas.isEmpty() ? null : googleMapsUrl(origem, paradas, modal);
        return new RotaResponse(v.getNome(), modal.name(), modal.getRotulo(), modal.getVelocidadeKmH(), origem, paradas,
                total, tempoEstimadoMin(total, modal), url);
    }

    /** Heurística do vizinho mais próximo: a cada passo, vai ao ponto ainda não visitado mais perto. */
    public static List<Ponto> vizinhoMaisProximo(Ponto origem, List<Ponto> pontos) {
        List<Ponto> restantes = new ArrayList<>(pontos);
        List<Ponto> ordem = new ArrayList<>();
        Ponto atual = origem;
        while (!restantes.isEmpty()) {
            Ponto ref = atual;
            Ponto maisPerto = Collections.min(restantes, Comparator.comparingDouble(
                    p -> Geo.distanciaMetros(ref.latitude(), ref.longitude(), p.latitude(), p.longitude())));
            ordem.add(maisPerto);
            restantes.remove(maisPerto);
            atual = maisPerto;
        }
        return ordem;
    }

    public static int tempoEstimadoMin(int distanciaM, Modal modal) {
        if (distanciaM <= 0) return 0;
        double horas = (distanciaM / 1000.0) / modal.getVelocidadeKmH();
        return (int) Math.max(1, Math.ceil(horas * 60));
    }

    /** https://www.google.com/maps/dir/?api=1&origin=...&destination=...&waypoints=...&travelmode=... (sem chave). */
    static String googleMapsUrl(RotaResponse.Ponto origem, List<RotaResponse.Parada> paradas, Modal modal) {
        RotaResponse.Parada destino = paradas.get(paradas.size() - 1);
        String waypoints = paradas.subList(0, paradas.size() - 1).stream()
                .map(p -> coord(p.latitude(), p.longitude()))
                .collect(Collectors.joining("|"));
        StringBuilder sb = new StringBuilder("https://www.google.com/maps/dir/?api=1")
                .append("&origin=").append(enc(coord(origem.latitude(), origem.longitude())))
                .append("&destination=").append(enc(coord(destino.latitude(), destino.longitude())));
        if (!waypoints.isEmpty()) sb.append("&waypoints=").append(enc(waypoints));
        sb.append("&travelmode=").append(modal.getTravelModeGoogle());
        return sb.toString();
    }

    private static String coord(double lat, double lng) {
        return String.format(Locale.ROOT, "%.6f,%.6f", lat, lng);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
