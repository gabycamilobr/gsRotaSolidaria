package br.com.techalimenta.rotasolidaria.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushTokenRequest(@NotBlank @Size(max = 512) String token) {
}
