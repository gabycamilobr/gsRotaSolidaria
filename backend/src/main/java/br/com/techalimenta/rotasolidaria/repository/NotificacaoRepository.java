package br.com.techalimenta.rotasolidaria.repository;

import br.com.techalimenta.rotasolidaria.domain.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findTop30ByUsuarioIdAndIdGreaterThanOrderByIdDesc(Long usuarioId, Long apos);

    long countByUsuarioIdAndLidaFalse(Long usuarioId);

    @Modifying
    @Query("update Notificacao n set n.lida = true where n.usuarioId = :usuarioId and n.lida = false")
    int marcarTodasComoLidas(@Param("usuarioId") Long usuarioId);
}
