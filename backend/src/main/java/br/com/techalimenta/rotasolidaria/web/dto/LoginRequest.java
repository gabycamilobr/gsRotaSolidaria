package br.com.techalimenta.rotasolidaria.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 120) String email,
        @NotBlank(message = "Informe a senha.") @Size(max = 100) String senha) {
}
