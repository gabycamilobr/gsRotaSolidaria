package br.com.techalimenta.rotasolidaria.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lote_foto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LoteFoto {
    @Id
    @Column(name = "lote_id")
    private Long loteId;

    @Column(columnDefinition = "bytea")
    private byte[] conteudo;

    private String tipo;
}
