# Arquitetura – Rota Solidária

Plataforma de bairro que liga pequenos comércios com sobra de alimento a ONGs, igrejas e associações num raio de 2 km, com retirada em menos de uma hora.

## Visão geral

```mermaid
flowchart LR
    subgraph Usuarios["Usuários (celular)"]
        COM["Comerciante<br/>(padaria, quitanda)"]
        REC["Receptor<br/>(ONG, igreja, associação)"]
        VOL["Voluntário"]
    end

    subgraph Azure["Microsoft Azure"]
        SWA["PWA React + Vite<br/>Azure Static Web Apps<br/><b>PaaS</b>"]
        API["API REST Spring Boot 3 · Java 21<br/>Azure App Service<br/><b>PaaS</b>"]
        DB[("PostgreSQL + PostGIS<br/>Azure Database for PostgreSQL<br/>Flexible Server · <b>PaaS</b>")]
    end

    FCM["Firebase Cloud Messaging<br/><b>SaaS</b>"]
    WA["Grupo de WhatsApp<br/>das famílias"]
    GMAPS["Google Maps<br/>(link de direções por URL)"]
    GH["GitHub Projects + Actions<br/><b>SaaS</b>"]

    COM & REC & VOL -->|HTTPS| SWA
    SWA -->|REST + JWT| API
    API -->|JPA · Flyway · ST_DWithin| DB
    API -->|push web| FCM
    FCM -->|aviso de lote próximo| REC
    REC -->|link wa.me com a mensagem pronta| WA
    VOL -->|rota ordenada| GMAPS
    GH -.->|CI/CD| SWA
    GH -.->|CI/CD| API
```

- **PWA (front-end)**: React + Vite, mobile-first e instalável (manifest + service worker do `vite-plugin-pwa`). Hospedado no **Azure Static Web Apps**, que entrega os arquivos estáticos por CDN com HTTPS.
- **API (back-end)**: Java 21 + Spring Boot 3 (Spring Web, Spring Data JPA, Bean Validation, Spring Security com JWT, `@Scheduled`), documentada com springdoc-openapi (Swagger UI em `/swagger-ui.html`). Hospedada no **Azure App Service** (Linux, Java SE).
- **Banco**: PostgreSQL com **PostGIS** (`geography(Point, 4326)` e índices GIST), migrações versionadas com **Flyway**. Destino: **Azure Database for PostgreSQL – Flexible Server**.
- **Notificações**: **Firebase Cloud Messaging** (push web). Sem credenciais, nada quebra: o aviso é gravado no banco e aparece dentro do app (polling leve a cada 15 s).
- **Famílias**: não usam o sistema. A ONG repassa o aviso pelo grupo de WhatsApp que já tem, com um link `https://wa.me/?text=...` (sem API paga). Nenhum dado de família é armazenado.
- **Rotas**: sem API paga. A rota é calculada na API (vizinho mais próximo) e aberta no Google Maps por URL (`https://www.google.com/maps/dir/?api=1&origin=...&destination=...&waypoints=...`), sem chave.
- **Gestão**: GitHub Projects (quadro montado a partir do [BACKLOG.md](../BACKLOG.md)) e GitHub Actions para o deploy.

## Fluxo principal (sequência)

```mermaid
sequenceDiagram
    autonumber
    participant C as Comerciante (PWA)
    participant A as API Spring Boot
    participant B as PostgreSQL + PostGIS
    participant F as Firebase (FCM)
    participant R as Receptor (PWA)
    participant W as WhatsApp da ONG
    participant V as Voluntário (PWA)

    C->>A: POST /api/lotes (foto JPEG comprimida + dados)
    A->>B: INSERT lote, lote_foto
    A->>B: ST_DWithin(2000 m) + categoria ORDER BY ST_Distance
    A->>B: INSERT lote_candidato (fila em ordem)
    A-->>F: push aos candidatos (se configurado)
    A->>B: INSERT notificacao (aviso dentro do app)
    R->>A: GET /api/notificacoes (a cada 15 s)
    R->>A: POST /api/lotes/{id}/reservas
    A->>B: SELECT ... FOR UPDATE + @Version
    A-->>R: código 4 dígitos + QR, expira em 45 min
    R->>W: wa.me/?text=mensagem pronta
    V->>A: GET /api/rota (vizinho mais próximo)
    V->>C: mostra o QR code
    C->>A: POST /api/retiradas (código)
    A->>B: lote RETIRADO + recibo
    Note over A,B: @Scheduled: reserva vencida → lote DISPONIVEL e próximo candidato avisado
```

## Por que PaaS e não IaaS

Escolhemos serviços gerenciados (PaaS) em vez de máquinas virtuais (IaaS). Com Static Web Apps, App Service e PostgreSQL Flexible Server **não há servidor para administrar**: a Azure cuida do sistema operacional, dos patches de segurança, do runtime Java e do certificado HTTPS, e a equipe de três pessoas gasta o tempo no produto, não em `apt upgrade`. **Backup e escala são gerenciados**: o PostgreSQL Flexible Server faz backup automático com restauração para um ponto no tempo, e o App Service escala verticalmente ou horizontalmente por configuração, sem reinstalar nada. Por fim, a solução **cabe nos créditos do Azure for Students**: o Static Web Apps tem plano gratuito, e o App Service (plano B1) e o PostgreSQL (camada Burstable B1ms) são as menores camadas pagas, suficientes para um piloto de bairro. Uma VM equivalente exigiria pagar e manter servidor, banco, backup e proxy reverso por conta própria.

## Decisões técnicas

| Tema | Decisão | Motivo |
|---|---|---|
| Localização | Colunas `latitude`/`longitude` mapeadas pelo JPA e coluna `localizacao geography(Point, 4326)` **gerada** pelo banco | O JPA não precisa de biblioteca espacial; o PostGIS indexa (GIST) e calcula distâncias em metros |
| Matching | Consulta nativa com `ST_DWithin` + `ST_Distance` ([docs/queries.sql](queries.sql)), fila gravada em `lote_candidato` | Rápida (índice GIST, < 500 ms) e legível para o vídeo |
| Reserva dupla | `SELECT ... FOR UPDATE` no lote + `@Version` + índice único parcial `reserva(lote_id) WHERE status='ATIVA'` | Três barreiras: aplicação, ORM e banco |
| Expiração | `@Scheduled` a cada 15 s (5 s no perfil demo) | Sem fila/broker extra; simples de explicar |
| Fotos | Comprimidas no navegador (1024 px, JPEG 0,7) e guardadas em `bytea` numa tabela separada (`lote_foto`) | Sem serviço de storage; listas não carregam os bytes |
| Push | FCM opcional; aviso sempre gravado em `notificacao` e entregue por polling | O sistema não depende de credencial externa para funcionar |
| Autenticação | JWT HS256 assinado pelo Spring Security (resource server), senhas com BCrypt | Stateless, sem servidor de identidade extra |
