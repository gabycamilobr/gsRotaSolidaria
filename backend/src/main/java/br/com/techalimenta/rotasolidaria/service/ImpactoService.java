package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.domain.Lote;
import br.com.techalimenta.rotasolidaria.domain.StatusLote;
import br.com.techalimenta.rotasolidaria.repository.LoteRepository;
import br.com.techalimenta.rotasolidaria.web.dto.ImpactoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

/**
 * Painel de impacto. Fatores fixos:
 * - refeições = kg / 0,54 (1 refeição = 0,54 kg – padrão Feeding America/USDA)
 * - CO₂e evitado = kg × 0,627 (fatores de conversão do governo do Reino Unido, BEIS/DESNZ)
 */
@Service
public class ImpactoService {

    public static final BigDecimal KG_POR_REFEICAO = new BigDecimal("0.54");
    public static final BigDecimal CO2E_POR_KG = new BigDecimal("0.627");
    public static final List<String> FONTES = List.of(
            "1 refeição = 0,54 kg – padrão Feeding America/USDA.",
            "0,627 kg CO₂e por kg de alimento desviado do aterro – fatores de conversão do governo do Reino Unido, BEIS/DESNZ.");

    private final LoteRepository lotes;

    public ImpactoService(LoteRepository lotes) {
        this.lotes = lotes;
    }

    @Transactional(readOnly = true)
    public ImpactoResponse geral() {
        return calcular("geral", lotes.findAll());
    }

    @Transactional(readOnly = true)
    public ImpactoResponse doComercio(Long comercioId) {
        return calcular("comercio", lotes.findDoComercio(comercioId));
    }

    public static long refeicoes(BigDecimal kg) {
        return kg.divide(KG_POR_REFEICAO, 0, RoundingMode.HALF_UP).longValue();
    }

    public static BigDecimal co2eEvitado(BigDecimal kg) {
        return kg.multiply(CO2E_POR_KG).setScale(1, RoundingMode.HALF_UP);
    }

    public static ImpactoResponse calcular(String escopo, List<Lote> todos) {
        List<Lote> retirados = todos.stream().filter(l -> l.getStatus() == StatusLote.RETIRADO).toList();
        BigDecimal kg = retirados.stream().map(Lote::getPesoKg).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);
        long publicados = todos.size();
        int percentual = publicados == 0 ? 0 : (int) Math.round(retirados.size() * 100.0 / publicados);
        Integer tempoMedio = retirados.stream()
                .filter(l -> l.getRetiradoEm() != null && l.getPublicadoEm() != null)
                .mapToLong(l -> Duration.between(l.getPublicadoEm(), l.getRetiradoEm()).toSeconds())
                .average()
                .stream().mapToObj(s -> (int) Math.round(s / 60.0)).findFirst().orElse(null);
        return new ImpactoResponse(escopo, kg, refeicoes(kg), co2eEvitado(kg), publicados, retirados.size(),
                percentual, tempoMedio, FONTES);
    }
}
