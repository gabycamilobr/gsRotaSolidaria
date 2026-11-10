package br.com.techalimenta.rotasolidaria.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "lote")
@Getter @Setter @NoArgsConstructor
public class Lote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comercio_id")
    private Comercio comercio;

    @Enumerated(EnumType.STRING)
    private Categoria categoria;
    private String descricao;
    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    private Unidade unidade;

    @Column(name = "peso_kg")
    private BigDecimal pesoKg;

    @Column(name = "retirar_ate")
    private Instant retirarAte;

    /** Declaração do comerciante: "Confirmo que o alimento está dentro da validade e foi conservado corretamente". */
    @Column(name = "confirmacao_validade")
    private boolean confirmacaoValidade;

    @Column(name = "confirmado_em")
    private Instant confirmadoEm;

    @Enumerated(EnumType.STRING)
    private StatusLote status = StatusLote.DISPONIVEL;

    private double latitude;
    private double longitude;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Column(name = "retirado_em")
    private Instant retiradoEm;

    /** Controle de concorrência otimista (segunda barreira contra reserva dupla). */
    @Version
    private long version;
}
