package br.com.techalimenta.rotasolidaria.web;

import br.com.techalimenta.rotasolidaria.security.UsuarioLogado;
import br.com.techalimenta.rotasolidaria.service.ReciboService;
import br.com.techalimenta.rotasolidaria.service.RetiradaService;
import br.com.techalimenta.rotasolidaria.web.dto.ReciboResponse;
import br.com.techalimenta.rotasolidaria.web.dto.RetiradaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Retirada e recibos")
public class RetiradaController {

    private final RetiradaService retiradas;
    private final ReciboService recibos;

    public RetiradaController(RetiradaService retiradas, ReciboService recibos) {
        this.retiradas = retiradas;
        this.recibos = recibos;
    }

    @PostMapping("/retiradas")
    @PreAuthorize("hasRole('COMERCIANTE')")
    @Operation(summary = "Confirma a retirada pelo código (QR ou 4 dígitos): lote vira RETIRADO e o recibo é gerado")
    public ReciboResponse confirmar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RetiradaRequest req) {
        return retiradas.confirmar(UsuarioLogado.de(jwt).exigirComercio(), req.codigo(), req.reservaId());
    }

    @GetMapping("/recibos")
    @Operation(summary = "Recibos do comércio (ou do receptor) logado")
    public List<ReciboResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return recibos.listar(UsuarioLogado.de(jwt));
    }

    @GetMapping("/recibos/{id}")
    @Operation(summary = "Recibo digital da doação")
    public ReciboResponse detalhe(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return recibos.detalhe(id, UsuarioLogado.de(jwt));
    }
}
