package br.com.techalimenta.rotasolidaria.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** ONG, igreja ou associação de moradores. O endereço é o ponto de distribuição às famílias. */
@Entity
@Table(name = "receptor")
@Getter @Setter @NoArgsConstructor
public class Receptor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String endereco;
    private double latitude;
    private double longitude;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "categorias_aceitas", columnDefinition = "varchar(30)[]")
    private String[] categoriasAceitas;
}
