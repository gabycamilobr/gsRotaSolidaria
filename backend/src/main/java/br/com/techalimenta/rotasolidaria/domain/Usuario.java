package br.com.techalimenta.rotasolidaria.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "usuario")
@Getter @Setter @NoArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;

    @Column(name = "senha_hash")
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comercio_id")
    private Comercio comercio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receptor_id")
    private Receptor receptor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voluntario_id")
    private Voluntario voluntario;

    /** LGPD: momento em que o titular consentiu com o uso da localização do estabelecimento. */
    @Column(name = "consentimento_localizacao_em")
    private Instant consentimentoLocalizacaoEm;

    @Column(name = "push_token")
    private String pushToken;

    public String nomeExibicao() {
        return switch (perfil) {
            case COMERCIANTE -> comercio.getNome();
            case RECEPTOR -> receptor.getNome();
            case VOLUNTARIO -> voluntario.getNome();
        };
    }
}
