package br.com.techalimenta.rotasolidaria.web.dto;

import br.com.techalimenta.rotasolidaria.domain.Perfil;
import br.com.techalimenta.rotasolidaria.domain.Usuario;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsuarioResponse(Long id, String email, String nome, Perfil perfil, String endereco,
                              Long comercioId, Long receptorId, Long voluntarioId,
                              String modal, String receptorVinculado) {

    public static UsuarioResponse de(Usuario u) {
        return switch (u.getPerfil()) {
            case COMERCIANTE -> new UsuarioResponse(u.getId(), u.getEmail(), u.getComercio().getNome(), u.getPerfil(),
                    u.getComercio().getEndereco(), u.getComercio().getId(), null, null, null, null);
            case RECEPTOR -> new UsuarioResponse(u.getId(), u.getEmail(), u.getReceptor().getNome(), u.getPerfil(),
                    u.getReceptor().getEndereco(), null, u.getReceptor().getId(), null, null, null);
            case VOLUNTARIO -> new UsuarioResponse(u.getId(), u.getEmail(), u.getVoluntario().getNome(), u.getPerfil(),
                    null, null, null, u.getVoluntario().getId(), u.getVoluntario().getModal().name(),
                    u.getVoluntario().getReceptor().getNome());
        };
    }
}
