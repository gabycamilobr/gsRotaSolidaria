package br.com.techalimenta.rotasolidaria.repository;

import br.com.techalimenta.rotasolidaria.domain.Lote;
import br.com.techalimenta.rotasolidaria.domain.StatusLote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    /** SELECT ... FOR UPDATE: serializa reservas concorrentes do mesmo lote. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lote l where l.id = :id")
    Optional<Lote> findByIdParaAtualizar(@Param("id") Long id);

    @Query("select l from Lote l join fetch l.comercio where l.comercio.id = :comercioId and l.publicadoEm >= :desde order by l.publicadoEm desc")
    List<Lote> findDoComercioDesde(@Param("comercioId") Long comercioId, @Param("desde") Instant desde);

    @Query("select l from Lote l where l.comercio.id = :comercioId")
    List<Lote> findDoComercio(@Param("comercioId") Long comercioId);

    @Query("""
            select l from Lote l join fetch l.comercio
            where l.status in :status
              and exists (select 1 from LoteCandidato c where c.loteId = l.id and c.receptorId = :receptorId)
            order by l.retirarAte
            """)
    List<Lote> findDisponiveisParaReceptor(@Param("receptorId") Long receptorId,
                                           @Param("status") Collection<StatusLote> status);

    @Query("select l.id from Lote l where l.status in :status and l.retirarAte < :agora")
    List<Long> findIdsComPrazoVencido(@Param("status") Collection<StatusLote> status, @Param("agora") Instant agora);
}
