package br.com.techalimenta.rotasolidaria.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Confirmação de retirada. Pode vir do QR code ("ROTASOLIDARIA:{reservaId}:{codigo}") ou
 * do código de 4 dígitos digitado pelo comerciante.
 */
public record RetiradaRequest(
        @NotBlank(message = "Informe o código de retirada.")
        @Pattern(regexp = "\\d{4}", message = "O código tem 4 dígitos.") String codigo,
        Long reservaId) {
}
