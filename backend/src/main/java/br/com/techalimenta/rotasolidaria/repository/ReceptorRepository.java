package br.com.techalimenta.rotasolidaria.repository;

import br.com.techalimenta.rotasolidaria.domain.Receptor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReceptorRepository extends JpaRepository<Receptor, Long> {

    /** Projeção usada pelo cartaz do dia. */
    interface ReceptorDistancia {
        Long getId();
        String getNome();
        String getEndereco();
        Number getDistanciaM();
    }

    /** Ponto de distribuição (receptor) mais próximo do comércio – usado no cartaz do dia. */
    @Query(value = """
            SELECT r.id AS id, r.nome AS nome, r.endereco AS endereco,
                   ROUND(ST_Distance(r.localizacao, c.localizacao)) AS distanciaM
            FROM receptor r JOIN comercio c ON c.id = :comercioId
            ORDER BY r.localizacao <-> c.localizacao
            LIMIT 1
            """, nativeQuery = true)
    Optional<ReceptorDistancia> maisProximoDoComercio(@Param("comercioId") Long comercioId);
}
