package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.domain.Categoria;
import br.com.techalimenta.rotasolidaria.domain.Lote;
import br.com.techalimenta.rotasolidaria.domain.StatusLote;
import br.com.techalimenta.rotasolidaria.domain.Unidade;
import br.com.techalimenta.rotasolidaria.web.dto.ImpactoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImpactoServiceTest {

    @Test
    @DisplayName("conferência do roteiro: 4 kg = 7 refeições e 2,5 kg de CO₂e")
    void quatroQuilos() {
        assertThat(ImpactoService.refeicoes(new BigDecimal("4"))).isEqualTo(7);       // 4 / 0,54 = 7,41
        assertThat(ImpactoService.co2eEvitado(new BigDecimal("4"))).isEqualByComparingTo("2.5"); // 4 × 0,627 = 2,508
    }

    @ParameterizedTest(name = "{0} kg -> {1} refeições, {2} kg CO₂e")
    @CsvSource({"0, 0, 0.0", "1, 2, 0.6", "0.27, 1, 0.2", "3, 6, 1.9", "10, 19, 6.3"})
    @DisplayName("arredondamento: refeições inteiras (meio para cima) e CO₂e com uma casa")
    void arredondamento(String kg, long refeicoes, String co2) {
        assertThat(ImpactoService.refeicoes(new BigDecimal(kg))).isEqualTo(refeicoes);
        assertThat(ImpactoService.co2eEvitado(new BigDecimal(kg))).isEqualByComparingTo(co2);
    }

    @Test
    @DisplayName("painel soma só os lotes RETIRADOS e calcula percentual e tempo médio")
    void painelCompleto() {
        Lote paes = Fixtures.paes(StatusLote.RETIRADO);
        paes.setRetiradoEm(paes.getPublicadoEm().plus(Duration.ofMinutes(30)));
        Lote frutas = Fixtures.lote(11L, Fixtures.quitanda(), Categoria.HORTIFRUTI, "frutas", "3", Unidade.KG, "3", StatusLote.RETIRADO);
        frutas.setRetiradoEm(frutas.getPublicadoEm().plus(Duration.ofMinutes(50)));
        Lote expirado = Fixtures.lote(12L, Fixtures.padaria(), Categoria.PANIFICADOS, "bolos", "2", Unidade.UNIDADES, "1.5", StatusLote.EXPIRADO);

        ImpactoResponse r = ImpactoService.calcular("geral", List.of(paes, frutas, expirado));

        assertThat(r.kgSalvos()).isEqualByComparingTo("4.0");
        assertThat(r.refeicoes()).isEqualTo(7);
        assertThat(r.co2eEvitadoKg()).isEqualByComparingTo("2.5");
        assertThat(r.lotesPublicados()).isEqualTo(3);
        assertThat(r.lotesRetirados()).isEqualTo(2);
        assertThat(r.percentualRetirado()).isEqualTo(67);
        assertThat(r.tempoMedioRetiradaMin()).isEqualTo(40);
        assertThat(r.fontes()).anyMatch(f -> f.contains("Feeding America/USDA")).anyMatch(f -> f.contains("BEIS/DESNZ"));
    }

    @Test
    @DisplayName("sem lotes, o painel mostra zeros e tempo médio vazio")
    void painelVazio() {
        ImpactoResponse r = ImpactoService.calcular("geral", List.of());
        assertThat(r.kgSalvos()).isEqualByComparingTo("0");
        assertThat(r.percentualRetirado()).isZero();
        assertThat(r.tempoMedioRetiradaMin()).isNull();
    }
}
