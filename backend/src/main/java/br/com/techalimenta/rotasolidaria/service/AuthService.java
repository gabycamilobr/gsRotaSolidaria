package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.config.AppProperties;
import br.com.techalimenta.rotasolidaria.domain.Usuario;
import br.com.techalimenta.rotasolidaria.exception.NegocioException;
import br.com.techalimenta.rotasolidaria.repository.UsuarioRepository;
import br.com.techalimenta.rotasolidaria.web.dto.LoginResponse;
import br.com.techalimenta.rotasolidaria.web.dto.UsuarioResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final AppProperties props;
    private final Clock clock;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtEncoder jwtEncoder,
                       AppProperties props, Clock clock) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String senha) {
        Usuario u = usuarios.findByEmailIgnoreCase(email.trim())
                .filter(x -> encoder.matches(senha, x.getSenhaHash()))
                .orElseThrow(() -> new BadCredentialsException("credenciais"));

        Instant agora = Instant.now(clock);
        Instant expira = agora.plus(props.jwt().expiracaoHoras(), ChronoUnit.HOURS);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("rota-solidaria")
                .subject(u.getId().toString())
                .issuedAt(agora)
                .expiresAt(expira)
                .claim("perfil", u.getPerfil().name());
        if (u.getComercio() != null) claims.claim("comercioId", u.getComercio().getId());
        if (u.getReceptor() != null) claims.claim("receptorId", u.getReceptor().getId());
        if (u.getVoluntario() != null) claims.claim("voluntarioId", u.getVoluntario().getId());

        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
        return new LoginResponse(token, expira, UsuarioResponse.de(u));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse eu(Long usuarioId) {
        return usuarios.findById(usuarioId).map(UsuarioResponse::de)
                .orElseThrow(() -> NegocioException.naoEncontrado("Usuário não encontrado."));
    }

    @Transactional
    public void registrarPushToken(Long usuarioId, String token) {
        Usuario u = usuarios.findById(usuarioId)
                .orElseThrow(() -> NegocioException.naoEncontrado("Usuário não encontrado."));
        u.setPushToken(token);
    }
}
