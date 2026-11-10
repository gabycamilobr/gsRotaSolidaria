package br.com.techalimenta.rotasolidaria.repository;

import br.com.techalimenta.rotasolidaria.domain.Voluntario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoluntarioRepository extends JpaRepository<Voluntario, Long> {
}
