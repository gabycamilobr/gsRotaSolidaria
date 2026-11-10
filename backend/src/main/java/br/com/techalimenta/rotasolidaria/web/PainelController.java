package br.com.techalimenta.rotasolidaria.web;

import br.com.techalimenta.rotasolidaria.security.UsuarioLogado;
import br.com.techalimenta.rotasolidaria.service.CartazService;
import br.com.techalimenta.rotasolidaria.service.ImpactoService;
import br.com.techalimenta.rotasolidaria.service.RotaService;
import br.com.techalimenta.rotasolidaria.web.dto.CartazResponse;
import br.com.techalimenta.rotasolidaria.web.dto.ImpactoResponse;
import br.com.techalimenta.rotasolidaria.web.dto.RotaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Rota, impacto e cartaz")
public class PainelController {

    private final RotaService rotas;
    private final ImpactoService impacto;
    private final CartazService cartaz;

    public PainelController(RotaService rotas, ImpactoService impacto, CartazService cartaz) {
        this.rotas = rotas;
        this.impacto = impacto;
        this.cartaz = cartaz;
    }

    @GetMapping("/rota")
    @PreAuthorize("hasRole('VOLUNTARIO')")
    @Operation(summary = "Rota de coleta do voluntário (vizinho mais próximo) com link do Google Maps")
    public RotaResponse rota(@AuthenticationPrincipal Jwt jwt,
                             @RequestParam(required = false) Double lat,
                             @RequestParam(required = false) Double lng) {
        return rotas.calcular(UsuarioLogado.de(jwt).exigirVoluntario(), lat, lng);
    }

    @GetMapping("/impacto/geral")
    @Operation(summary = "Painel de impacto geral da plataforma")
    public ImpactoResponse geral() {
        return impacto.geral();
    }

    @GetMapping("/impacto/meu")
    @PreAuthorize("hasRole('COMERCIANTE')")
    @Operation(summary = "Painel de impacto do comércio logado")
    public ImpactoResponse meu(@AuthenticationPrincipal Jwt jwt) {
        return impacto.doComercio(UsuarioLogado.de(jwt).exigirComercio());
    }

    @GetMapping("/cartaz")
    @PreAuthorize("hasRole('COMERCIANTE')")
    @Operation(summary = "Dados do cartaz do dia (A4 imprimível)")
    public CartazResponse cartaz(@AuthenticationPrincipal Jwt jwt) {
        return cartaz.doDia(UsuarioLogado.de(jwt).exigirComercio());
    }
}
