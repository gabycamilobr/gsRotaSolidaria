# Rota Solidária

**Da sobra ao prato, no mesmo bairro, em menos de uma hora.**

Protótipo funcional da equipe **TechAlimenta** para a Global Solution 2026.2 da FIAP (desafio **CitizenTech**).

Pequenos comércios (padarias, quitandas, mercadinhos) publicam a sobra do dia em até 30 segundos. ONGs, igrejas e associações de moradores num raio de 2 km são avisadas, reservam e repassam o aviso às famílias pelo WhatsApp que já usam. Um voluntário recebe a rota de coleta. As famílias não se cadastram e nenhum dado delas é armazenado.

| Perfil | O que faz no app |
|---|---|
| **Comerciante** (quem doa) | Publica lote, confirma a retirada (QR code ou código), vê recibos, imprime o cartaz do dia e acompanha o painel de impacto |
| **Receptor** (ONG, igreja, associação) | Vê lotes próximos, reserva, compartilha no WhatsApp e mostra o QR code na retirada |
| **Voluntário** (quem transporta) | Vê a rota de coleta em ordem, com link do Google Maps |

---

## Sumário

1. [Stack e estrutura](#stack-e-estrutura)
2. [Como rodar localmente](#como-rodar-localmente)
3. [Usuários de demonstração](#usuários-de-demonstração)
4. [Perfil demo e roteiro do vídeo](#perfil-demo-e-roteiro-do-vídeo)
5. [Variáveis de ambiente](#variáveis-de-ambiente)
6. [Testes](#testes)
7. [Deploy na Azure](#deploy-na-azure)
8. [Decisões registradas](#decisões-registradas)
9. [Documentação](#documentação)

---

## Stack e estrutura

| Camada | Tecnologia | Hospedagem |
|---|---|---|
| Front-end | PWA em React 19 + Vite (JavaScript), `vite-plugin-pwa` | Azure Static Web Apps |
| Back-end | Java 21, Spring Boot 3.5 (Web, Data JPA, Bean Validation, Security com JWT, `@Scheduled`), springdoc-openapi | Azure App Service |
| Banco | PostgreSQL 16 + PostGIS (`geography(Point, 4326)`), Flyway | Azure Database for PostgreSQL – Flexible Server |
| Notificações | Firebase Cloud Messaging (push web), com fallback de aviso dentro do app | Firebase (SaaS) |
| Testes | JUnit 5 + Mockito (+ testes contra PostGIS real) | GitHub Actions |
| Gestão | GitHub Projects ([BACKLOG.md](BACKLOG.md)) | GitHub |

```
/frontend   PWA React (Vite)
/backend    API Spring Boot (Maven)
/docs       arquitetura, requisitos não funcionais, consultas SQL do vídeo
/.github    workflows de deploy (Static Web Apps e App Service)
README.md   este arquivo
BACKLOG.md  issues para o GitHub Projects
docker-compose.yml   PostgreSQL + PostGIS SOMENTE para desenvolvimento local
```

---

## Como rodar localmente

### Pré-requisitos

- **Java 21** e **Maven 3.9+**
- **Node.js 20.19+ ou 22** e npm
- **PostgreSQL 16 com PostGIS**, de um destes jeitos:
  - **Docker** (mais simples) – usa o `docker-compose.yml`, que é **apenas para desenvolvimento**;
  - ou PostgreSQL instalado na máquina com o pacote do PostGIS (no Ubuntu: `sudo apt install postgresql-16-postgis-3`).

### 1. Banco de dados

**Com Docker (somente dev):**

```bash
docker compose up -d
```

**Sem Docker** (PostgreSQL local com PostGIS instalado):

```bash
sudo -u postgres psql -c "CREATE USER rota WITH PASSWORD 'rota';"
```

```bash
sudo -u postgres psql -c "CREATE DATABASE rotasolidaria OWNER rota;"
```

```bash
sudo -u postgres psql -d rotasolidaria -c "CREATE EXTENSION postgis;"
```

> O `CREATE EXTENSION postgis` precisa de superusuário no PostgreSQL local; por isso ele é feito aqui uma vez. Depois, as migrações do Flyway (`CREATE EXTENSION IF NOT EXISTS`) só confirmam que ela existe. Não é preciso criar tabelas: o **Flyway** cria o esquema e carrega os dados de demonstração quando a API sobe.

### 2. API (back-end)

```bash
cd backend
mvn spring-boot:run
```

- API em `http://localhost:8080`
- **Swagger UI**: http://localhost:8080/swagger-ui.html (faça login em `POST /api/auth/login`, copie o `token` e use o botão **Authorize**)
- Health check: http://localhost:8080/actuator/health

Os valores padrão já apontam para `jdbc:postgresql://localhost:5432/rotasolidaria` com usuário e senha `rota`. Para mudar, veja as [variáveis de ambiente](#variáveis-de-ambiente).

### 3. PWA (front-end)

Em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

Abra http://localhost:5173. Em desenvolvimento o Vite encaminha `/api` para `http://localhost:8080`, então não é preciso configurar nada.

Para testar o PWA como em produção (service worker ativo, instalável):

```bash
npm run build && npm run preview
```

Abra http://localhost:4173. Para conferir o tamanho do bundle inicial: `npm run tamanho`.

> **Dica para celular na mesma rede Wi-Fi**: `npm run dev -- --host` e acesse pelo IP do computador. Câmera e push exigem HTTPS fora de `localhost`; para testar a leitura do QR code no celular, use o deploy na Azure (HTTPS) ou digite o código de 4 dígitos.

---

## Usuários de demonstração

Todos com a senha **`demo123`**. Na tela de abertura há botões **"Entrar como"** que fazem o login com um toque.

| Perfil | E-mail | Entidade | Observação |
|---|---|---|---|
| Comerciante | `padaria@demo.com` | Padaria Bom Dia | Rua Domingos de Morais, 1500 – Vila Mariana |
| Comerciante | `quitanda@demo.com` | Quitanda da Vila | Rua Joaquim Távora, 520 |
| Receptor | `maosdadas@demo.com` | Associação Mãos Dadas | ~400 m da padaria, aceita todas as categorias |
| Receptor | `esperanca@demo.com` | Comunidade Esperança | ~1,2 km, aceita Panificados e Hortifrúti |
| Receptor | `vilanova@demo.com` | Casa de Apoio Vila Nova | ~2,8 km, **fora do raio** (mostra o filtro) |
| Voluntário | `ana@demo.com` | Ana | de bicicleta, vinculada à Associação Mãos Dadas |

Dados fictícios, com coordenadas aproximadas de ruas reais da Vila Mariana (São Paulo).

---

## Perfil demo e roteiro do vídeo

### Ativar o perfil `demo`

No perfil `demo` a reserva vale **1 minuto** (em vez de 45) e o agendador verifica as reservas a cada 5 s, para mostrar a expiração no vídeo.

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Ou, com o jar: `java -jar target/rota-solidaria-api.jar --spring.profiles.active=demo`. Na Azure: App Setting `SPRING_PROFILES_ACTIVE=demo`.

O tempo da reserva também pode ser ajustado sem perfil: `app.reserva.minutos` no `application.yml` (ou `--app.reserva.minutos=5`).

### Recomeçar do zero

Apaga lotes, reservas, recibos e avisos, mantendo os usuários do seed:

```bash
psql -h localhost -U rota -d rotasolidaria -f docs/reset-demo.sql
```

### Roteiro (critério de aceite final)

Testado ponta a ponta pela interface, a 360 px de largura.

1. **Publicar** – entre como **Padaria Bom Dia** → **Publicar sobra** → tire a foto → Panificados já vem marcado e o item vem como "pães franceses" → quantidade **20** (o peso aparece **1 kg** sozinho) → "Retirar até" **21:00** (padrão) → marque a confirmação de validade → **Publicar e avisar organizações**. A tela mostra o tempo gasto e as organizações avisadas: Associação Mãos Dadas (402 m) e Comunidade Esperança (1,2 km).
2. **Consulta no banco** – rode a consulta de [`docs/queries.sql`](docs/queries.sql):
   ```bash
   psql -h localhost -U rota -d rotasolidaria -v loteId=1 -f docs/queries.sql
   ```
   Aparecem Mãos Dadas (402 m) e Esperança (1.184 m), em ordem, sem a Casa de Apoio Vila Nova (2.791 m). A segunda consulta do arquivo mostra por que a Vila Nova ficou de fora, e o `EXPLAIN ANALYZE` mostra o índice GIST.
3. **Reservar** – entre como **Associação Mãos Dadas**: o aviso aparece no sino (e como notificação em até 15 s). Abra o lote (distância 402 m, retirar até 21:00) → **Reservar este lote** → começa a contagem de 45:00, aparecem o QR code, o código de 4 dígitos e **Compartilhar no WhatsApp**, que abre a mensagem pronta.
   **Expiração (perfil demo)**: sem retirada em 1 minuto, o lote volta a DISPONIVEL e a **Comunidade Esperança** recebe "Lote disponível de novo" (entre como Esperança para mostrar).
4. **Rota** – entre como **Quitanda da Vila**, publique **3 kg de frutas** (Hortifrúti, unidade kg) e reserve-o como Associação Mãos Dadas. Entre como **Ana**: a rota mostra Padaria Bom Dia → Quitanda da Vila → Associação Mãos Dadas (~930 m, ~4 min de bicicleta) e o botão **Abrir no Google Maps**.
5. **Retirada e impacto** – como **Padaria**, vá em **Retirada** e leia o QR code (ou digite o código) → "Retirada confirmada" → **Ver recibo** (imprimível/PDF). Repita na **Quitanda** com o código das frutas. Em **Impacto → Geral**: **4,0 kg**, **≈ 7 refeições** e **≈ 2,5 kg de CO₂e**.

> **Como gravar com o perfil demo**: com reserva de 1 minuto não dá tempo de fazer os passos 4 e 5. Sugestão: grave os passos 1 a 5 com o perfil padrão (45 min) e grave a cena da expiração à parte, com o perfil `demo` e um lote separado (por exemplo, 5 pães de forma), depois de rodar o `reset-demo.sql`.

---

## Variáveis de ambiente

### API (`backend`)

| Variável | Padrão (dev) | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/rotasolidaria` | JDBC do PostgreSQL. Na Azure, acrescente `?sslmode=require` |
| `DB_USER` / `DB_PASSWORD` | `rota` / `rota` | Credenciais do banco |
| `DB_POOL` | `5` | Tamanho do pool de conexões |
| `JWT_SECRET` | segredo de dev | **Obrigatório em produção**, mínimo 32 caracteres |
| `JWT_EXPIRACAO_HORAS` | `12` | Validade do token |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:4173` | Domínio(s) do front, separados por vírgula |
| `FIREBASE_CREDENTIALS` | vazio | **Opcional.** Caminho do JSON da conta de serviço do Firebase **ou** o JSON em Base64. Vazio = sem push; os avisos aparecem dentro do app |
| `SPRING_PROFILES_ACTIVE` | vazio | `demo` para reserva de 1 minuto |
| `PORT` | `8080` | Porta HTTP (o App Service também pode definir `SERVER_PORT`) |

Configurações de negócio no `application.yml`: `app.reserva.minutos` (45), `app.matching.raio-metros` (2000), `app.agendador.intervalo-ms` (15000).

### PWA (`frontend`, arquivo `.env` – veja `.env.example`)

| Variável | Descrição |
|---|---|
| `VITE_API_URL` | URL da API. Vazio em dev (proxy do Vite). Em produção, a URL do App Service |
| `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_PROJECT_ID`, `VITE_FIREBASE_MESSAGING_SENDER_ID`, `VITE_FIREBASE_APP_ID`, `VITE_FIREBASE_VAPID_KEY` | **Opcionais.** Configuração web do Firebase para o push. Sem elas, o botão de ativar notificações não aparece e os avisos chegam por polling a cada 15 s |

### Ativar o push (opcional)

1. No [console do Firebase](https://console.firebase.google.com), crie um projeto e adicione um **app Web**; copie `apiKey`, `projectId`, `messagingSenderId` e `appId` para as variáveis `VITE_FIREBASE_*`.
2. Em **Configurações do projeto → Cloud Messaging → Certificados push da Web**, gere o par de chaves e copie a chave pública para `VITE_FIREBASE_VAPID_KEY`.
3. Em **Configurações do projeto → Contas de serviço**, gere uma chave privada (JSON). Na API, aponte `FIREBASE_CREDENTIALS` para o arquivo ou cole o conteúdo em Base64 (`base64 -w0 arquivo.json`). **Nunca versione esse arquivo** (o `.gitignore` já bloqueia os nomes comuns).
4. No app, toque no sino → **Ativar notificações no celular**.

Sem Firebase, nada quebra: a API registra `Firebase não configurado... avisos serão entregues apenas dentro do app` e o aviso aparece no sino e como notificação na tela.

---

## Testes

```bash
cd backend
mvn test
```

Testes unitários (JUnit 5 + Mockito):

| Classe | O que cobre |
|---|---|
| `MatchingServiceTest` | fila de candidatos em ordem de distância e avisos; nenhum candidato |
| `ReservaServiceTest` | reserva com sucesso; **bloqueio de reserva dupla**; lock pessimista; fora do raio; reserva limitada ao "retirar até"; **expiração devolvendo o lote e avisando o próximo** (excluindo quem deixou expirar) |
| `ImpactoServiceTest` | **4 kg → 7 refeições e 2,5 kg CO₂e**, arredondamento, percentual e tempo médio |
| `RotaServiceTest` | **ordenação da rota** (vizinho mais próximo), tempo por modal, rota da Ana e link do Google Maps |
| `RetiradaServiceTest` | retirada, recibo com o texto da Lei 14.016/2020, código inválido, reserva vencida |
| `LoteServiceTest` | categorias não aceitas, janela de 2 h dos refrigerados, confirmação obrigatória |

Testes contra **PostgreSQL + PostGIS real** (rodam quando `ROTA_TEST_DB_URL` está definida, como no GitHub Actions):

| Classe | O que cobre |
|---|---|
| `MatchingConsultaPostgisTest` | **dentro e fora do raio, categoria aceita e não aceita** com a consulta `ST_DWithin` real e o seed; tempo < 500 ms |
| `ReservaConcorrenciaPostgisTest` | duas reservas simultâneas: uma vence, a outra recebe conflito |

```bash
createdb -h localhost -U rota rotasolidaria_test
ROTA_TEST_DB_URL=jdbc:postgresql://localhost:5432/rotasolidaria_test mvn test
```

Com o Docker do `docker-compose.yml`, crie o banco de testes com `docker exec -it rota-solidaria-postgres-dev createdb -U rota rotasolidaria_test`. Sem Docker, crie-o como no passo 1 (`CREATE DATABASE rotasolidaria_test OWNER rota` e `CREATE EXTENSION postgis` como superusuário).

---

## Deploy na Azure

Arquitetura 100 % PaaS (detalhes e justificativa em [docs/arquitetura.md](docs/arquitetura.md)). Exemplo com a Azure CLI (troque os nomes, que precisam ser únicos, e as senhas):

### 1. Grupo de recursos

```bash
az group create --name rg-rota-solidaria --location brazilsouth
```

### 2. PostgreSQL Flexible Server com PostGIS liberado

```bash
az postgres flexible-server create --resource-group rg-rota-solidaria --name psql-rota-solidaria --location brazilsouth --tier Burstable --sku-name Standard_B1ms --storage-size 32 --version 16 --admin-user rotaadmin --admin-password '<SENHA-FORTE>' --public-access 0.0.0.0
```

> **Importante – liberar o PostGIS antes do `CREATE EXTENSION`.** No Azure Database for PostgreSQL – Flexible Server, só as extensões listadas no parâmetro de servidor **`azure.extensions`** podem ser criadas. Se o PostGIS não estiver nessa lista, a primeira migração do Flyway (`CREATE EXTENSION IF NOT EXISTS postgis`) falha e a API não sobe. Libere-o **antes** do primeiro deploy da API (no portal: *Parâmetros do servidor → azure.extensions → marcar POSTGIS → Salvar*), ou pela CLI:

```bash
az postgres flexible-server parameter set --resource-group rg-rota-solidaria --server-name psql-rota-solidaria --name azure.extensions --value POSTGIS
```

```bash
az postgres flexible-server db create --resource-group rg-rota-solidaria --server-name psql-rota-solidaria --database-name rotasolidaria
```

O `--public-access 0.0.0.0` libera o acesso a partir de serviços da Azure (o App Service). Para rodar o `docs/queries.sql` da sua máquina, adicione uma regra de firewall para o seu IP.

### 3. API no App Service (Java 21)

```bash
az appservice plan create --resource-group rg-rota-solidaria --name plan-rota-solidaria --is-linux --sku B1
```

```bash
az webapp create --resource-group rg-rota-solidaria --plan plan-rota-solidaria --name api-rota-solidaria --runtime "JAVA:21-java21"
```

(Confira o nome do runtime com `az webapp list-runtimes --os linux`.)

```bash
az webapp config appsettings set --resource-group rg-rota-solidaria --name api-rota-solidaria --settings DB_URL='jdbc:postgresql://psql-rota-solidaria.postgres.database.azure.com:5432/rotasolidaria?sslmode=require' DB_USER=rotaadmin DB_PASSWORD='<SENHA-FORTE>' JWT_SECRET='<SEGREDO-COM-32+-CARACTERES>' CORS_ORIGINS='https://<seu-static-web-app>.azurestaticapps.net'
```

```bash
az webapp config set --resource-group rg-rota-solidaria --name api-rota-solidaria --generic-configurations '{"healthCheckPath": "/actuator/health"}'
```

Opcional: `FIREBASE_CREDENTIALS` (JSON da conta de serviço em Base64) e `SPRING_PROFILES_ACTIVE=demo` para a gravação.

### 4. PWA no Static Web Apps

```bash
az staticwebapp create --resource-group rg-rota-solidaria --name swa-rota-solidaria --location eastus2 --sku Free
```

O arquivo `frontend/public/staticwebapp.config.json` já configura o fallback de rotas do SPA e o cache dos arquivos.

### 5. GitHub Actions

Os workflows em `.github/workflows` publicam a cada push na `main`:

| Workflow | O que faz | Configure no repositório |
|---|---|---|
| `backend-azure-app-service.yml` | `mvn verify` (com um PostGIS temporário só para os testes) e deploy do jar | secret `AZURE_WEBAPP_PUBLISH_PROFILE` (App Service → *Baixar perfil de publicação*) e variável `AZURE_WEBAPP_NAME` |
| `frontend-azure-static-web-apps.yml` | `npm ci`, build, checagem do bundle ≤ 300 KB e deploy | secret `AZURE_STATIC_WEB_APPS_API_TOKEN` (Static Web App → *Gerenciar token de implantação*), variável `VITE_API_URL` e, se usar push, as variáveis `VITE_FIREBASE_*` |

Depois do primeiro deploy, atualize `CORS_ORIGINS` da API com a URL final do Static Web App.

---

## Decisões registradas

Pontos em que o enunciado deixava margem; escolhemos a opção mais simples que mantém o roteiro funcionando.

1. **Campo "Item"** na publicação (ex.: "pães franceses"), pré-preenchido pela categoria. Ele é necessário para a mensagem do WhatsApp ("hoje temos 20 pães franceses") e para o recibo.
2. **Aviso na publicação**: todos os receptores do raio que aceitam a categoria são avisados ao mesmo tempo, em ordem de distância; quem reservar primeiro leva. Na expiração, só o **próximo da fila** é avisado de novo, pulando quem já deixou a reserva desse lote expirar.
3. O receptor que deixou a reserva expirar **não é avisado de novo**, mas o lote volta a aparecer para todos os candidatos e qualquer um deles pode reservá-lo (primeiro que chegar).
4. A reserva **nunca passa do "retirar até"** do lote: vale 45 min ou até o horário-limite, o que vier antes.
5. O lote só aparece para os receptores que estavam no raio **no momento da publicação** (fila gravada em `lote_candidato`).
6. **"Retirar até"** é um horário do dia (campo de hora), no máximo 24 h depois da publicação; refrigerados no máximo 2 h. Lotes que passam do horário sem retirada viram EXPIRADO.
7. **Peso estimado** por categoria: panificados 0,05 kg/unidade (20 pães ≈ 1 kg), hortifrúti 0,2 kg/unidade, industrializados 0,5 kg/unidade, refrigerados 0,3 kg/unidade; bandejas e kg com fatores próprios. Sempre editável.
8. **Localização**: o JPA grava latitude/longitude e o PostgreSQL gera a coluna `geography(Point, 4326)` (coluna gerada), sem biblioteca espacial no Java. O lote guarda uma cópia da posição do comércio, porque a consulta do enunciado usa `l.localizacao`.
9. **Rota**: distâncias em linha reta (haversine) e tempo pela velocidade do modal, arredondado para cima. O trajeto real fica a cargo do Google Maps, aberto pelo link com `travelmode`.
10. **QR code**: o conteúdo é `ROTASOLIDARIA:{idDaReserva}:{código}`. O voluntário também vê o QR de cada coleta na tela da rota, para mostrar no comércio.
11. **Cartaz do dia**: lista os lotes publicados hoje (disponíveis, reservados ou retirados), usa o maior "retirar até" como horário e indica o receptor mais próximo do comércio como ponto de distribuição.
12. **Painel**: "lotes publicados" conta todos os lotes (inclusive cancelados e expirados); "% retirado" = retirados ÷ publicados. O painel geral considera todos os comércios.
13. **Avisos**: todo aviso é gravado na tabela `notificacao` e aparece no sino/notificação do app por polling de 15 s (só com a aba visível). O push do FCM é enviado depois do commit, quando configurado.
14. **Cadastro**: não há tela de cadastro no escopo; os usuários vêm do seed, com o consentimento de localização registrado em `usuario.consentimento_localizacao_em`.
15. **Fotos**: o endpoint `GET /api/lotes/{id}/foto` é público (fotos só de alimento) para que a tag `<img>` e o cache do service worker funcionem sem token. Ficam numa tabela separada (`lote_foto`) para as listas não carregarem os bytes.
16. **Sessão** guardada no `localStorage` do navegador (protótipo); o token expira em 12 h.
17. **Recibos** também ficam disponíveis ao receptor pela API (`GET /api/recibos`); no app, a tela de recibos é do comerciante.

---

## Documentação

- [docs/arquitetura.md](docs/arquitetura.md) – diagramas Mermaid, PaaS × IaaS e decisões técnicas
- [docs/requisitos-nao-funcionais.md](docs/requisitos-nao-funcionais.md) – desempenho, mobile-first, acessibilidade, LGPD, segurança, disponibilidade e matching < 500 ms, com evidências
- [docs/queries.sql](docs/queries.sql) – consulta de matching para o vídeo e consultas de apoio
- [docs/reset-demo.sql](docs/reset-demo.sql) – limpa os dados para regravar a demonstração
- [BACKLOG.md](BACKLOG.md) – issues para o GitHub Projects
- Swagger UI: `/swagger-ui.html` na API

### Equipe TechAlimenta

- **Gabrielly** – front-end e protótipo
- **Luccas** – back-end e banco
- **Victor** – pesquisa, pitch e vídeo
