package br.com.techalimenta.rotasolidaria.service;

import br.com.techalimenta.rotasolidaria.config.AppProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import com.google.firebase.messaging.WebpushNotification;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Envia push web pelo Firebase Cloud Messaging.
 * Sem credenciais configuradas (FIREBASE_CREDENTIALS vazio), o sistema NÃO quebra:
 * apenas registra um log e o aviso continua disponível dentro do app (polling a cada 15 s).
 */
@Component
public class FcmPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);

    private final AppProperties props;
    private FirebaseMessaging messaging;

    public FcmPushSender(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    void iniciar() {
        String credenciais = props.firebase() == null ? null : props.firebase().credenciais();
        if (credenciais == null || credenciais.isBlank()) {
            log.warn("Firebase não configurado (FIREBASE_CREDENTIALS vazio): avisos serão entregues apenas dentro do app.");
            return;
        }
        try (InputStream in = abrir(credenciais.trim())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
            messaging = FirebaseMessaging.getInstance(app);
            log.info("Firebase Cloud Messaging ativo.");
        } catch (Exception e) {
            log.error("Não foi possível iniciar o Firebase ({}). Seguindo só com avisos dentro do app.", e.getMessage());
            messaging = null;
        }
    }

    private InputStream abrir(String credenciais) throws Exception {
        if (credenciais.startsWith("{")) {
            return new ByteArrayInputStream(credenciais.getBytes());
        }
        Path caminho = Path.of(credenciais);
        if (Files.exists(caminho)) {
            return new FileInputStream(caminho.toFile());
        }
        return new ByteArrayInputStream(Base64.getDecoder().decode(credenciais));
    }

    @Override
    public boolean ativo() {
        return messaging != null;
    }

    @Override
    public boolean enviar(String token, String titulo, String mensagem, Long loteId) {
        if (messaging == null) {
            log.info("[aviso no app] Push desligado; aviso '{}' ficou apenas dentro do app.", titulo);
            return false;
        }
        try {
            Message msg = Message.builder()
                    .setToken(token)
                    .putData("titulo", titulo)
                    .putData("mensagem", mensagem)
                    .putData("loteId", loteId == null ? "" : loteId.toString())
                    .setWebpushConfig(WebpushConfig.builder()
                            .setNotification(WebpushNotification.builder()
                                    .setTitle(titulo)
                                    .setBody(mensagem)
                                    .setIcon("/icons/icon-192.png")
                                    .setBadge("/icons/badge-72.png")
                                    .build())
                            .setFcmOptions(WebpushFcmOptions.withLink("/"))
                            .build())
                    .build();
            messaging.send(msg);
            return true;
        } catch (Exception e) {
            log.warn("Falha ao enviar push FCM ({}). O aviso continua disponível dentro do app.", e.getMessage());
            return false;
        }
    }
}
