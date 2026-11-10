package br.com.techalimenta.rotasolidaria.repository;

import br.com.techalimenta.rotasolidaria.domain.Recibo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReciboRepository extends JpaRepository<Recibo, Long> {

    @Query("select r from Recibo r where r.lote.comercio.id = :comercioId order by r.emitidoEm desc")
    List<Recibo> findDoComercio(@Param("comercioId") Long comercioId);

    @Query("""
            select rc from Recibo rc, Reserva rs
            where rs.lote = rc.lote and rs.status = 'CONCLUIDA' and rs.receptor.id = :receptorId
            order by rc.emitidoEm desc
            """)
    List<Recibo> findDoReceptor(@Param("receptorId") Long receptorId);

    Optional<Recibo> findByLoteId(Long loteId);
}
