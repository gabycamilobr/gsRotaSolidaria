package br.com.techalimenta.rotasolidaria.web;

import br.com.techalimenta.rotasolidaria.security.UsuarioLogado;
import br.com.techalimenta.rotasolidaria.service.ReservaService;
import br.com.techalimenta.rotasolidaria.web.dto.ReservaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas")
public class ReservaController {

    private final ReservaService reservas;

    public ReservaController(ReservaService reservas) {
        this.reservas = reservas;
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('RECEPTOR')")
    @Operation(summary = "Reservas do receptor logado (ativas e histórico), com QR code e código")
    public List<ReservaResponse> minhas(@AuthenticationPrincipal Jwt jwt) {
        return reservas.doReceptor(UsuarioLogado.de(jwt).exigirReceptor());
    }
}
