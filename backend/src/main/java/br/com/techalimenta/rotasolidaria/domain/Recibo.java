package br.com.techalimenta.rotasolidaria.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Recibo digital da doação. Guarda uma cópia dos dados no momento da retirada. */
@Entity
@Table(name = "recibo")
@Getter @Setter @NoArgsConstructor
public class Recibo {
    public static final String TEXTO_LEGAL = "Doação realizada de boa-fé, nos termos da Lei nº 14.016/2020.";
    public static final String TEXTO_CONFIRMACAO =
            "Confirmo que o alimento está dentro da validade e foi conservado corretamente.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    private String numero;
    @Column(name = "emitido_em") private Instant emitidoEm;
    @Column(name = "comercio_nome") private String comercioNome;
    @Column(name = "comercio_endereco") private String comercioEndereco;
    private String item;
    @Enumerated(EnumType.STRING) private Categoria categoria;
    private BigDecimal quantidade;
    @Enumerated(EnumType.STRING) private Unidade unidade;
    @Column(name = "peso_kg") private BigDecimal pesoKg;
    @Column(name = "receptor_nome") private String receptorNome;
    @Column(name = "receptor_endereco") private String receptorEndereco;
    @Column(name = "confirmacao_validade") private String confirmacaoValidade;
    @Column(name = "confirmado_em") private Instant confirmadoEm;
    @Column(name = "texto_legal") private String textoLegal;
}
