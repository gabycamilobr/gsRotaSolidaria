package br.com.techalimenta.rotasolidaria.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Aviso dentro do app. Sempre gravado; o push do Firebase é um canal adicional. */
@Entity
@Table(name = "notificacao")
@Getter @Setter @NoArgsConstructor
public class Notificacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "usuario_id") private Long usuarioId;
    private String titulo;
    private String mensagem;
    @Column(name = "lote_id") private Long loteId;
    @Column(name = "criada_em") private Instant criadaEm;
    private boolean lida;
}
