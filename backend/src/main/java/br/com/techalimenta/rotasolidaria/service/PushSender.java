package br.com.techalimenta.rotasolidaria.service;

/** Canal de push (Firebase Cloud Messaging). Implementações nunca devem lançar exceção. */
public interface PushSender {

    /** @return true se o push foi aceito pelo provedor. */
    boolean enviar(String token, String titulo, String mensagem, Long loteId);

    boolean ativo();
}
