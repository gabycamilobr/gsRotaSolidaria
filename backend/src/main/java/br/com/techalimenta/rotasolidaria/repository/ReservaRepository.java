package br.com.techalimenta.rotasolidaria.repository;

import br.com.techalimenta.rotasolidaria.domain.Reserva;
import br.com.techalimenta.rotasolidaria.domain.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Optional<Reserva> findFirstByLoteIdAndStatus(Long loteId, StatusReserva status);

    List<Reserva> findByLoteIdInAndStatus(Collection<Long> loteIds, StatusReserva status);

    @Query("select r.id from Reserva r where r.status = 'ATIVA' and r.expiraEm < :agora")
    List<Long> findIdsVencidas(@Param("agora") Instant agora);

    @Query("select r.receptor.id from Reserva r where r.lote.id = :loteId and r.status = 'EXPIRADA'")
    Set<Long> findReceptoresQueDeixaramExpirar(@Param("loteId") Long loteId);

    @Query("""
            select r from Reserva r join fetch r.lote l join fetch l.comercio join fetch r.receptor
            where r.receptor.id = :receptorId
            order by r.criadaEm desc
            """)
    List<Reserva> findDoReceptor(@Param("receptorId") Long receptorId);

    @Query("""
            select r from Reserva r join fetch r.lote l join fetch l.comercio join fetch r.receptor
            where r.receptor.id = :receptorId and r.status = 'ATIVA'
            """)
    List<Reserva> findAtivasDoReceptor(@Param("receptorId") Long receptorId);

    @Query("""
            select r from Reserva r join fetch r.lote l join fetch l.comercio join fetch r.receptor
            where l.comercio.id = :comercioId and r.status = 'ATIVA' and r.codigo = :codigo
            """)
    List<Reserva> findAtivasDoComercioPorCodigo(@Param("comercioId") Long comercioId, @Param("codigo") String codigo);

    @Query("select r.codigo from Reserva r where r.lote.comercio.id = :comercioId and r.status = 'ATIVA'")
    Set<String> findCodigosAtivosDoComercio(@Param("comercioId") Long comercioId);
}
