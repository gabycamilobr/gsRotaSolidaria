package br.com.techalimenta.rotasolidaria.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "reserva")
@Getter @Setter @NoArgsConstructor
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receptor_id")
    private Receptor receptor;

    /** Código de retirada de 4 dígitos (também codificado no QR code). */
    private String codigo;

    @Enumerated(EnumType.STRING)
    private StatusReserva status = StatusReserva.ATIVA;

    @Column(name = "criada_em")
    private Instant criadaEm;

    @Column(name = "expira_em")
    private Instant expiraEm;

    @Column(name = "encerrada_em")
    private Instant encerradaEm;

    /** Conteúdo do QR code lido pelo comerciante. */
    public String qrConteudo() {
        return "ROTASOLIDARIA:" + id + ":" + codigo;
    }
}
