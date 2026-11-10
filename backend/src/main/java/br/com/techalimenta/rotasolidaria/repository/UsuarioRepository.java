package br.com.techalimenta.rotasolidaria.repository;

import br.com.techalimenta.rotasolidaria.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findByReceptorId(Long receptorId);

    List<Usuario> findByComercioId(Long comercioId);

    @Query("select u from Usuario u where u.voluntario.receptor.id = :receptorId")
    List<Usuario> findVoluntariosDoReceptor(@Param("receptorId") Long receptorId);
}
