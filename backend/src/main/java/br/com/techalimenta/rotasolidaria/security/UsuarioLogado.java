package br.com.techalimenta.rotasolidaria.security;

import br.com.techalimenta.rotasolidaria.domain.Perfil;
import br.com.techalimenta.rotasolidaria.exception.NegocioException;
import org.springframework.security.oauth2.jwt.Jwt;

/** Dados do usuário autenticado, extraídos do JWT. */
public record UsuarioLogado(Long usuarioId, Perfil perfil, Long comercioId, Long receptorId, Long voluntarioId) {

    public static UsuarioLogado de(Jwt jwt) {
        return new UsuarioLogado(
                Long.valueOf(jwt.getSubject()),
                Perfil.valueOf(jwt.getClaimAsString("perfil")),
                claimLong(jwt, "comercioId"),
                claimLong(jwt, "receptorId"),
                claimLong(jwt, "voluntarioId"));
    }

    private static Long claimLong(Jwt jwt, String nome) {
        Object valor = jwt.getClaim(nome);
        return valor == null ? null : Long.valueOf(valor.toString());
    }

    public Long exigirComercio() {
        if (comercioId == null) throw NegocioException.proibido("Ação disponível apenas para comerciantes.");
        return comercioId;
    }

    public Long exigirReceptor() {
        if (receptorId == null) throw NegocioException.proibido("Ação disponível apenas para receptores.");
        return receptorId;
    }

    public Long exigirVoluntario() {
        if (voluntarioId == null) throw NegocioException.proibido("Ação disponível apenas para voluntários.");
        return voluntarioId;
    }
}
