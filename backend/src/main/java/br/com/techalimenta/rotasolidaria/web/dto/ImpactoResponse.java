package br.com.techalimenta.rotasolidaria.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record ImpactoResponse(
        String escopo,
        BigDecimal kgSalvos,
        long refeicoes,
        BigDecimal co2eEvitadoKg,
        long lotesPublicados,
        long lotesRetirados,
        int percentualRetirado,
        Integer tempoMedioRetiradaMin,
        List<String> fontes) {
}
