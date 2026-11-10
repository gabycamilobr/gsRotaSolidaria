package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.domain.Recibo;
import br.com.techalimenta.rotasolidaria.domain.Reserva;
import br.com.techalimenta.rotasolidaria.domain.StatusReserva;
import br.com.techalimenta.rotasolidaria.exception.NegocioException;
import br.com.techalimenta.rotasolidaria.repository.ReciboRepository;
import br.com.techalimenta.rotasolidaria.repository.ReservaRepository;
import br.com.techalimenta.rotasolidaria.security.UsuarioLogado;
import br.com.techalimenta.rotasolidaria.web.dto.ReciboResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReciboService {

    private final ReciboRepository recibos;
    private final ReservaRepository reservas;

    public ReciboService(ReciboRepository recibos, ReservaRepository reservas) {
        this.recibos = recibos;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public List<ReciboResponse> listar(UsuarioLogado u) {
        List<Recibo> lista = switch (u.perfil()) {
            case COMERCIANTE -> recibos.findDoComercio(u.comercioId());
            case RECEPTOR -> recibos.findDoReceptor(u.receptorId());
            case VOLUNTARIO -> List.of();
        };
        return lista.stream().map(ReciboResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ReciboResponse detalhe(Long id, UsuarioLogado u) {
        Recibo r = recibos.findById(id).orElseThrow(() -> NegocioException.naoEncontrado("Recibo não encontrado."));
        boolean podeVer = switch (u.perfil()) {
            case COMERCIANTE -> r.getLote().getComercio().getId().equals(u.comercioId());
            case RECEPTOR -> reservas.findFirstByLoteIdAndStatus(r.getLote().getId(), StatusReserva.CONCLUIDA)
                    .map(Reserva::getReceptor).map(x -> x.getId().equals(u.receptorId())).orElse(false);
            case VOLUNTARIO -> false;
        };
        if (!podeVer) throw NegocioException.proibido("Você não tem acesso a este recibo.");
        return ReciboResponse.de(r);
    }
}
