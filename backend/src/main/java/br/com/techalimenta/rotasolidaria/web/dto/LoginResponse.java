package br.com.techalimenta.rotasolidaria.web.dto;

import java.time.Instant;

public record LoginResponse(String token, Instant expiraEm, UsuarioResponse usuario) {
}
