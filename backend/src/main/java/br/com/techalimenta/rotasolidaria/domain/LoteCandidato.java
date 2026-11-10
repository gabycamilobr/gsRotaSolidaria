package br.com.techalimenta.rotasolidaria.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Receptor selecionado pelo matching para um lote, com a posição na fila (ordem de distância). */
@Entity
@Table(name = "lote_candidato")
@IdClass(LoteCandidato.Chave.class)
@Getter @Setter @NoArgsConstructor
public class LoteCandidato {
    @Id
    @Column(name = "lote_id")
    private Long loteId;

    @Id
    @Column(name = "receptor_id")
    private Long receptorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receptor_id", insertable = false, updatable = false)
    private Receptor receptor;

    private int ordem;

    @Column(name = "distancia_m")
    private int distanciaM;

    @Column(name = "notificado_em")
    private Instant notificadoEm;

    public LoteCandidato(Long loteId, Long receptorId, int ordem, int distanciaM) {
        this.loteId = loteId;
        this.receptorId = receptorId;
        this.ordem = ordem;
        this.distanciaM = distanciaM;
    }

    @Getter @Setter @NoArgsConstructor
    public static class Chave implements Serializable {
        private Long loteId;
        private Long receptorId;

        @Override
        public boolean equals(Object o) {
            return o instanceof Chave c && Objects.equals(loteId, c.loteId) && Objects.equals(receptorId, c.receptorId);
        }

        @Override
        public int hashCode() { return Objects.hash(loteId, receptorId); }
    }
}
