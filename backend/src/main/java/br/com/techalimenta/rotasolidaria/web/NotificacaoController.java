package br.com.techalimenta.rotasolidaria.web;

import br.com.techalimenta.rotasolidaria.repository.NotificacaoRepository;
import br.com.techalimenta.rotasolidaria.security.UsuarioLogado;
import br.com.techalimenta.rotasolidaria.service.AuthService;
import br.com.techalimenta.rotasolidaria.service.PushSender;
import br.com.techalimenta.rotasolidaria.web.dto.NotificacaoResponse;
import br.com.techalimenta.rotasolidaria.web.dto.PushTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Avisos e push")
public class NotificacaoController {

    private final NotificacaoRepository notificacoes;
    private final AuthService auth;
    private final PushSender push;

    public NotificacaoController(NotificacaoRepository notificacoes, AuthService auth, PushSender push) {
        this.notificacoes = notificacoes;
        this.auth = auth;
        this.push = push;
    }

    @GetMapping("/notificacoes")
    @Operation(summary = "Avisos do usuário (polling leve a cada 15 s). Use 'apos' para buscar só os novos")
    public Map<String, Object> listar(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") Long apos) {
        Long usuarioId = UsuarioLogado.de(jwt).usuarioId();
        List<NotificacaoResponse> lista = notificacoes.findTop30ByUsuarioIdAndIdGreaterThanOrderByIdDesc(usuarioId, apos)
                .stream().map(NotificacaoResponse::de).toList();
        return Map.of("itens", lista, "naoLidas", notificacoes.countByUsuarioIdAndLidaFalse(usuarioId));
    }

    @PostMapping("/notificacoes/lidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    @Operation(summary = "Marca todos os avisos como lidos")
    public void marcarLidas(@AuthenticationPrincipal Jwt jwt) {
        notificacoes.marcarTodasComoLidas(UsuarioLogado.de(jwt).usuarioId());
    }

    @PostMapping("/push/token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Registra o token FCM do navegador para receber push")
    public void registrarToken(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PushTokenRequest req) {
        auth.registrarPushToken(UsuarioLogado.de(jwt).usuarioId(), req.token());
    }

    @GetMapping("/push/status")
    @Operation(summary = "Informa se o push (Firebase) está ativo no servidor")
    public Map<String, Boolean> status() {
        return Map.of("fcmAtivo", push.ativo());
    }
}
