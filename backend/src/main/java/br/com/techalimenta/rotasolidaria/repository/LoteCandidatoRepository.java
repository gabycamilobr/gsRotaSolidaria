package br.com.techalimenta.rotasolidaria.repository;

import br.com.techalimenta.rotasolidaria.domain.LoteCandidato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoteCandidatoRepository extends JpaRepository<LoteCandidato, LoteCandidato.Chave> {

    /** Linha devolvida pela consulta geográfica de matching. */
    interface ReceptorProximo {
        Long getReceptorId();
        String getNome();
        Number getDistanciaM();
    }

    /**
     * O coração do sistema: receptores a até {@code raio} metros do lote que aceitam a categoria,
     * em ordem de distância. ST_DWithin usa o índice GIST das colunas geography.
     * A mesma consulta, em versão legível, está em /docs/queries.sql.
     */
    @Query(value = """
            SELECT r.id AS receptorId, r.nome AS nome,
                   ROUND(ST_Distance(r.localizacao, l.localizacao)) AS distanciaM
            FROM receptor r JOIN lote l ON l.id = :loteId
            WHERE ST_DWithin(r.localizacao, l.localizacao, :raio)
              AND l.categoria = ANY(r.categorias_aceitas)
            ORDER BY distanciaM
            """, nativeQuery = true)
    List<ReceptorProximo> buscarReceptoresProximos(@Param("loteId") Long loteId, @Param("raio") double raio);

    @Query("select c from LoteCandidato c join fetch c.receptor where c.loteId = :loteId order by c.ordem")
    List<LoteCandidato> findByLoteIdOrdenado(@Param("loteId") Long loteId);

    Optional<LoteCandidato> findByLoteIdAndReceptorId(Long loteId, Long receptorId);

    List<LoteCandidato> findByReceptorIdAndLoteIdIn(Long receptorId, List<Long> loteIds);
}
