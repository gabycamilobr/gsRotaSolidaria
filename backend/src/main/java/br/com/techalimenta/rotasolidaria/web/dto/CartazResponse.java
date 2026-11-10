package br.com.techalimenta.rotasolidaria.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CartazResponse(
        String comercioNome,
        String comercioEndereco,
        LocalDate data,
        List<Item> itens,
        Instant horarioAte,
        PontoDistribuicao pontoDistribuicao) {

    public record Item(String descricao, BigDecimal quantidade, String unidade, String categoria, Instant retirarAte) {}

    public record PontoDistribuicao(String nome, String endereco, Integer distanciaM) {}
}
