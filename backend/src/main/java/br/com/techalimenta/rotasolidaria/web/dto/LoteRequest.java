package br.com.techalimenta.rotasolidaria.web.dto;

import br.com.techalimenta.rotasolidaria.domain.Categoria;
import br.com.techalimenta.rotasolidaria.domain.Unidade;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record LoteRequest(
        @NotNull(message = "Escolha a categoria.") Categoria categoria,
        @NotBlank(message = "Descreva o item (ex.: pães franceses).") @Size(max = 80, message = "Use até 80 caracteres.")
        String descricao,
        @NotNull(message = "Informe a quantidade.") @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero.")
        @DecimalMax(value = "10000", message = "Quantidade acima do permitido.") BigDecimal quantidade,
        @NotNull(message = "Escolha a unidade.") Unidade unidade,
        @NotNull(message = "Informe o peso estimado.") @DecimalMin(value = "0.01", message = "O peso deve ser maior que zero.")
        @DecimalMax(value = "1000", message = "Peso acima do permitido.") BigDecimal pesoKg,
        @NotNull(message = "Informe até que horas o lote pode ser retirado.")
        @Future(message = "O horário de retirada já passou.") Instant retirarAte,
        @AssertTrue(message = "Confirme que o alimento está dentro da validade e foi conservado corretamente.")
        boolean confirmacaoValidade) {
}
