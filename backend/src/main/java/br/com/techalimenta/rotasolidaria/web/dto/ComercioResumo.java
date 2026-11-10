package br.com.techalimenta.rotasolidaria.web.dto;

import br.com.techalimenta.rotasolidaria.domain.Comercio;

public record ComercioResumo(Long id, String nome, String endereco, double latitude, double longitude) {
    public static ComercioResumo de(Comercio c) {
        return new ComercioResumo(c.getId(), c.getNome(), c.getEndereco(), c.getLatitude(), c.getLongitude());
    }
}
