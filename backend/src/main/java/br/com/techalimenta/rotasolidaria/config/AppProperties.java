package br.com.techalimenta.rotasolidaria.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Configurações do Rota Solidária (prefixo "app" no application.yml). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Reserva reserva,
        Matching matching,
        Jwt jwt,
        Cors cors,
        Firebase firebase,
        String fuso) {

    public record Reserva(int minutos) {}

    public record Matching(double raioMetros) {}

    public record Jwt(String segredo, int expiracaoHoras) {}

    public record Cors(List<String> origens) {}

    /** credenciais: caminho do JSON da conta de serviço OU o próprio JSON em Base64. Vazio = desligado. */
    public record Firebase(String credenciais) {}
}
