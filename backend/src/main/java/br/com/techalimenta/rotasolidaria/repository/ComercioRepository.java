package br.com.techalimenta.rotasolidaria.repository;

import br.com.techalimenta.rotasolidaria.domain.Comercio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComercioRepository extends JpaRepository<Comercio, Long> {
}
