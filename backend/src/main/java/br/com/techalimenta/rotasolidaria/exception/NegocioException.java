package br.com.techalimenta.rotasolidaria.exception;

import org.springframework.http.HttpStatus;

/** Erro de regra de negócio com mensagem pronta para o usuário (em português). */
public class NegocioException extends RuntimeException {
    private final HttpStatus status;

    public NegocioException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static NegocioException naoEncontrado(String mensagem) {
        return new NegocioException(HttpStatus.NOT_FOUND, mensagem);
    }

    public static NegocioException conflito(String mensagem) {
        return new NegocioException(HttpStatus.CONFLICT, mensagem);
    }

    public static NegocioException regra(String mensagem) {
        return new NegocioException(HttpStatus.UNPROCESSABLE_ENTITY, mensagem);
    }

    public static NegocioException proibido(String mensagem) {
        return new NegocioException(HttpStatus.FORBIDDEN, mensagem);
    }
}
