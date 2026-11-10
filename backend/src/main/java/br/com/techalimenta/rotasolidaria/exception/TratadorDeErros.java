package br.com.techalimenta.rotasolidaria.exception;

import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Converte exceções em respostas RFC 7807 (ProblemDetail) com mensagens em português. */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(NegocioException.class)
    public ProblemDetail negocio(NegocioException e) {
        return ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Confira os campos destacados.");
        pd.setProperty("campos", campos);
        return pd;
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class,
            DataIntegrityViolationException.class})
    public ProblemDetail concorrencia(Exception e) {
        log.info("Conflito de concorrência: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Outra pessoa alterou este lote ao mesmo tempo. Atualize a tela e tente de novo.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail credenciais(BadCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail acessoNegado(AccessDeniedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Seu perfil não tem acesso a esta ação.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail fotoGrande(MaxUploadSizeExceededException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "A foto é grande demais (máximo 2 MB).");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail parteFaltando(MissingServletRequestPartException e) {
        String msg = "foto".equals(e.getRequestPartName()) ? "Tire ou escolha uma foto do lote." : "Dados do lote ausentes.";
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, msg);
    }
}
