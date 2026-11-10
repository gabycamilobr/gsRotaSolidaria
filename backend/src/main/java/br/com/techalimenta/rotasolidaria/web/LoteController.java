package br.com.techalimenta.rotasolidaria.web;

import br.com.techalimenta.rotasolidaria.domain.LoteFoto;
import br.com.techalimenta.rotasolidaria.security.UsuarioLogado;
import br.com.techalimenta.rotasolidaria.service.LoteService;
import br.com.techalimenta.rotasolidaria.service.ReservaService;
import br.com.techalimenta.rotasolidaria.web.dto.LoteRequest;
import br.com.techalimenta.rotasolidaria.web.dto.LoteResponse;
import br.com.techalimenta.rotasolidaria.web.dto.ReservaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/lotes")
@Tag(name = "Lotes")
public class LoteController {

    private final LoteService lotes;
    private final ReservaService reservas;

    public LoteController(LoteService lotes, ReservaService reservas) {
        this.lotes = lotes;
        this.reservas = reservas;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMERCIANTE')")
    @Operation(summary = "Publica um lote (foto + dados) e executa o matching; devolve os receptores avisados")
    public LoteResponse publicar(@AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestPart("lote") LoteRequest lote,
                                 @RequestPart("foto") MultipartFile foto) {
        return lotes.publicar(UsuarioLogado.de(jwt).exigirComercio(), lote, foto);
    }

    @GetMapping("/meus")
    @PreAuthorize("hasRole('COMERCIANTE')")
    @Operation(summary = "Lotes publicados hoje pelo comércio logado")
    public List<LoteResponse> meus(@AuthenticationPrincipal Jwt jwt) {
        return lotes.doComercioHoje(UsuarioLogado.de(jwt).exigirComercio());
    }

    @GetMapping("/proximos")
    @PreAuthorize("hasRole('RECEPTOR')")
    @Operation(summary = "Lotes disponíveis para o receptor logado (resultado do matching), por distância")
    public List<LoteResponse> proximos(@AuthenticationPrincipal Jwt jwt) {
        return lotes.proximosDoReceptor(UsuarioLogado.de(jwt).exigirReceptor());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um lote")
    public LoteResponse detalhe(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return lotes.detalhe(id, UsuarioLogado.de(jwt));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('COMERCIANTE')")
    @Operation(summary = "Cancela um lote disponível ou reservado")
    public LoteResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return lotes.cancelar(UsuarioLogado.de(jwt).exigirComercio(), id);
    }

    @PostMapping("/{id}/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RECEPTOR')")
    @Operation(summary = "Reserva o lote (o primeiro que reservar leva). Gera QR code e código de 4 dígitos")
    public ReservaResponse reservar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return reservas.reservar(UsuarioLogado.de(jwt).exigirReceptor(), id);
    }

    @GetMapping("/{id}/foto")
    @Operation(summary = "Foto do lote (JPEG comprimido no navegador)", security = {})
    public ResponseEntity<byte[]> foto(@PathVariable Long id) {
        LoteFoto foto = lotes.foto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getTipo()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic().immutable())
                .body(foto.getConteudo());
    }
}
