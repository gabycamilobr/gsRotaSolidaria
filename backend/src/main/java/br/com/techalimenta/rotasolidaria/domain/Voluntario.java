package br.com.techalimenta.rotasolidaria.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "voluntario")
@Getter @Setter @NoArgsConstructor
public class Voluntario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receptor_id")
    private Receptor receptor;

    @Enumerated(EnumType.STRING)
    private Modal modal;
    private double latitude;
    private double longitude;
}
