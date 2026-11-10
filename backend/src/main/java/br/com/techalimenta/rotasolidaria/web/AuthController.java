package br.com.techalimenta.rotasolidaria.web;

import br.com.techalimenta.rotasolidaria.security.UsuarioLogado;
import br.com.techalimenta.rotasolidaria.service.AuthService;
import br.com.techalimenta.rotasolidaria.web.dto.LoginRequest;
import br.com.techalimenta.rotasolidaria.web.dto.LoginResponse;
import br.com.techalimenta.rotasolidaria.web.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    @Operation(summary = "Login com e-mail e senha; devolve o JWT", security = {})
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.email(), req.senha());
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário logado")
    public UsuarioResponse eu(@AuthenticationPrincipal Jwt jwt) {
        return auth.eu(UsuarioLogado.de(jwt).usuarioId());
    }
}
