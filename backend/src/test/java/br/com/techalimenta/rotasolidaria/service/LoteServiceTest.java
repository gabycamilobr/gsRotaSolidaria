package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.domain.Categoria;
import br.com.techalimenta.rotasolidaria.domain.Unidade;
import br.com.techalimenta.rotasolidaria.exception.NegocioException;
import br.com.techalimenta.rotasolidaria.web.dto.LoteRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoteServiceTest {

    final LoteService service = new LoteService(null, null, null, null, null, null, null, Fixtures.props(45),
            Clock.fixed(Fixtures.AGORA, ZoneOffset.UTC));
    final MockMultipartFile foto = new MockMultipartFile("foto", "f.jpg", "image/jpeg", new byte[]{1, 2, 3});

    LoteRequest req(Categoria c, Duration janela, boolean confirmacao) {
        return new LoteRequest(c, "item", BigDecimal.TEN, Unidade.UNIDADES, BigDecimal.ONE, Fixtures.AGORA.plus(janela), confirmacao);
    }

    @Test
    @DisplayName("aceita panificados com foto, confirmação e retirada no futuro")
    void aceitaPanificados() {
        assertThatCode(() -> service.validar(req(Categoria.PANIFICADOS, Duration.ofHours(3), true), foto, Fixtures.AGORA))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("recusa refeições prontas e vencidos (Lei 14.016/2020)")
    void recusaCategoriasNaoAceitas() {
        assertThatThrownBy(() -> service.validar(req(Categoria.REFEICOES_PRONTAS, Duration.ofHours(1), true), foto, Fixtures.AGORA))
                .isInstanceOf(NegocioException.class).hasMessageContaining("Refeições prontas");
        assertThatThrownBy(() -> service.validar(req(Categoria.VENCIDOS, Duration.ofHours(1), true), foto, Fixtures.AGORA))
                .isInstanceOf(NegocioException.class).hasMessageContaining("vencidos");
    }

    @Test
    @DisplayName("refrigerados: janela de retirada de no máximo 2 horas")
    void refrigeradosDuasHoras() {
        assertThatCode(() -> service.validar(req(Categoria.REFRIGERADOS, Duration.ofMinutes(120), true), foto, Fixtures.AGORA))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.validar(req(Categoria.REFRIGERADOS, Duration.ofMinutes(121), true), foto, Fixtures.AGORA))
                .hasMessageContaining("2 horas");
    }

    @Test
    @DisplayName("exige a confirmação de validade/conservação e uma foto")
    void exigeConfirmacaoEFoto() {
        assertThatThrownBy(() -> service.validar(req(Categoria.PANIFICADOS, Duration.ofHours(1), false), foto, Fixtures.AGORA))
                .hasMessageContaining("validade");
        assertThatThrownBy(() -> service.validar(req(Categoria.PANIFICADOS, Duration.ofHours(1), true), null, Fixtures.AGORA))
                .hasMessageContaining("foto");
    }

    @Test
    @DisplayName("recusa horário de retirada que já passou")
    void horarioPassado() {
        LoteRequest r = new LoteRequest(Categoria.PANIFICADOS, "pães", BigDecimal.TEN, Unidade.UNIDADES, BigDecimal.ONE,
                Instant.from(Fixtures.AGORA).minusSeconds(60), true);
        assertThatThrownBy(() -> service.validar(r, foto, Fixtures.AGORA)).hasMessageContaining("já passou");
    }
}
