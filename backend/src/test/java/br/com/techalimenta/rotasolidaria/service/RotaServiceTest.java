package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.domain.*;
import br.com.techalimenta.rotasolidaria.repository.ReservaRepository;
import br.com.techalimenta.rotasolidaria.repository.VoluntarioRepository;
import br.com.techalimenta.rotasolidaria.service.RotaService.Ponto;
import br.com.techalimenta.rotasolidaria.web.dto.RotaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RotaServiceTest {

    @Mock VoluntarioRepository voluntarios;
    @Mock ReservaRepository reservas;
    @InjectMocks RotaService service;

    @Test
    @DisplayName("vizinho mais próximo: sempre vai ao ponto não visitado mais perto")
    void vizinhoMaisProximo() {
        Ponto origem = new Ponto("origem", 0, 0);
        List<Ponto> pontos = List.of(new Ponto("longe", 0, 0.03), new Ponto("perto", 0, 0.01), new Ponto("meio", 0, 0.02));

        assertThat(RotaService.vizinhoMaisProximo(origem, pontos)).extracting(Ponto::chave)
                .containsExactly("perto", "meio", "longe");
    }

    @Test
    @DisplayName("vizinho mais próximo não é só ordenar pela distância à origem")
    void vizinhoMaisProximoMudaDeReferencia() {
        // B é o mais perto da origem; de B, C (do mesmo lado) fica mais perto que A (do outro lado)
        Ponto origem = new Ponto("origem", 0, 0);
        List<Ponto> pontos = List.of(new Ponto("A", 0, -0.012), new Ponto("B", 0, 0.010), new Ponto("C", 0, 0.025));

        assertThat(RotaService.vizinhoMaisProximo(origem, pontos)).extracting(Ponto::chave)
                .containsExactly("B", "C", "A");
    }

    @Test
    @DisplayName("tempo estimado pelas velocidades do modal (a pé 5, bicicleta 15, carro 25 km/h)")
    void tempoEstimado() {
        assertThat(RotaService.tempoEstimadoMin(1000, Modal.A_PE)).isEqualTo(12);
        assertThat(RotaService.tempoEstimadoMin(1000, Modal.BICICLETA)).isEqualTo(4);
        assertThat(RotaService.tempoEstimadoMin(1000, Modal.CARRO)).isEqualTo(3);
        assertThat(RotaService.tempoEstimadoMin(0, Modal.CARRO)).isZero();
    }

    @Test
    @DisplayName("rota da Ana: Padaria, Quitanda e termina na Associação Mãos Dadas, com link do Google Maps")
    void rotaDaDemonstracao() {
        Receptor maosDadas = Fixtures.maosDadas();
        Voluntario ana = new Voluntario();
        ana.setId(1L);
        ana.setNome("Ana");
        ana.setModal(Modal.BICICLETA);
        ana.setReceptor(maosDadas);
        ana.setLatitude(-23.58700);
        ana.setLongitude(-46.63350);
        when(voluntarios.findById(1L)).thenReturn(Optional.of(ana));

        Lote frutas = Fixtures.lote(11L, Fixtures.quitanda(), Categoria.HORTIFRUTI, "frutas", "3", Unidade.KG, "3", StatusLote.RESERVADO);
        Lote paes = Fixtures.paes(StatusLote.RESERVADO);
        when(reservas.findAtivasDoReceptor(1L)).thenReturn(List.of(reserva(1L, frutas, maosDadas, "1111"), reserva(2L, paes, maosDadas, "2222")));

        RotaResponse rota = service.calcular(1L, null, null);

        assertThat(rota.paradas()).extracting(RotaResponse.Parada::nome)
                .containsExactly("Padaria Bom Dia", "Quitanda da Vila", "Associação Mãos Dadas");
        assertThat(rota.paradas()).extracting(RotaResponse.Parada::tipo).containsExactly("COLETA", "COLETA", "ENTREGA");
        assertThat(rota.paradas().get(0).itens()).extracting(RotaResponse.Item::codigo).containsExactly("2222");
        int soma = rota.paradas().stream().mapToInt(RotaResponse.Parada::distanciaTrechoM).sum();
        assertThat(rota.distanciaTotalM()).isEqualTo(soma).isBetween(850, 1050);
        assertThat(rota.tempoEstimadoMin()).isEqualTo(RotaService.tempoEstimadoMin(soma, Modal.BICICLETA));
        assertThat(rota.googleMapsUrl())
                .startsWith("https://www.google.com/maps/dir/?api=1&origin=-23.587000%2C-46.633500")
                .contains("&destination=-23.592700%2C-46.635200")
                .contains("&waypoints=-23.589100%2C-46.634700%7C-23.590500%2C-46.637500")
                .endsWith("&travelmode=bicycling");
    }

    @Test
    @DisplayName("sem reservas ativas, a rota vem vazia e sem link")
    void semColetas() {
        Voluntario ana = new Voluntario();
        ana.setNome("Ana");
        ana.setModal(Modal.A_PE);
        ana.setReceptor(Fixtures.maosDadas());
        when(voluntarios.findById(1L)).thenReturn(Optional.of(ana));
        when(reservas.findAtivasDoReceptor(1L)).thenReturn(List.of());

        RotaResponse rota = service.calcular(1L, null, null);
        assertThat(rota.paradas()).isEmpty();
        assertThat(rota.googleMapsUrl()).isNull();
    }

    private static Reserva reserva(Long id, Lote lote, Receptor receptor, String codigo) {
        Reserva r = new Reserva();
        r.setId(id);
        r.setLote(lote);
        r.setReceptor(receptor);
        r.setCodigo(codigo);
        r.setStatus(StatusReserva.ATIVA);
        r.setExpiraEm(Fixtures.AGORA.plusSeconds(1800));
        return r;
    }
}
