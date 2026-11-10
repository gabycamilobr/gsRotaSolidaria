package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.domain.Lote;
import br.com.techalimenta.rotasolidaria.domain.Unidade;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formatação de textos curtos em português usados nos avisos. */
public final class Textos {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private Textos() {}

    public static String numero(BigDecimal valor) {
        DecimalFormat f = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(PT_BR));
        return f.format(valor);
    }

    public static String quantidade(Lote lote) {
        String q = numero(lote.getQuantidade());
        if (lote.getUnidade() == Unidade.KG) return q + " kg de " + lote.getDescricao();
        if (lote.getUnidade() == Unidade.BANDEJAS) {
            return q + (lote.getQuantidade().compareTo(BigDecimal.ONE) == 0 ? " bandeja de " : " bandejas de ") + lote.getDescricao();
        }
        return q + " " + lote.getDescricao();
    }

    public static String distancia(int metros) {
        if (metros < 1000) return metros + " m";
        return numero(BigDecimal.valueOf(Math.round(metros / 100.0) / 10.0)) + " km";
    }
}
