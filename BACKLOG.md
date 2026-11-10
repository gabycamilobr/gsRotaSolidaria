# Backlog – Rota Solidária

Issues para montar o quadro no **GitHub Projects**. Sugestão de colunas: `Backlog` → `A fazer` → `Em andamento` → `Em revisão` → `Pronto`.
Sugestão de labels: `front-end`, `back-end`, `banco`, `pesquisa`, `pitch`, `documentação`, `infra`, `teste`.

**Responsáveis**
- **Gabrielly** – front-end e protótipo
- **Luccas** – back-end e banco
- **Victor** – pesquisa, pitch e vídeo

> Para criar as issues rapidamente: copie cada bloco como uma issue (título = `###`), aplique a label e o responsável, e adicione ao Project.

---

## Épico 1 – Banco de dados e dados de demonstração

### 1. Esquema PostgreSQL + PostGIS com Flyway
- **Descrição**: criar as tabelas `comercio`, `receptor`, `voluntario`, `usuario`, `lote`, `lote_foto`, `lote_candidato`, `reserva`, `recibo` e `notificacao`, com colunas `geography(Point, 4326)` e índices GIST.
- **Critério de aceite**: `V1__esquema_inicial.sql` roda do zero com `CREATE EXTENSION postgis`; o Hibernate sobe com `ddl-auto: validate` sem erro.
- **Responsável**: Luccas · `banco`

### 2. Seed da Vila Mariana
- **Descrição**: comércios (Padaria Bom Dia, Quitanda da Vila), receptores (Mãos Dadas ~400 m, Esperança ~1,2 km, Vila Nova ~2,8 km), voluntária Ana (bicicleta) e um usuário por perfil com senha `demo123`.
- **Critério de aceite**: a consulta de `docs/queries.sql` para um lote da padaria devolve Mãos Dadas e Esperança, nessa ordem, sem a Vila Nova.
- **Responsável**: Luccas · `banco`

### 3. Consulta de matching legível para o vídeo
- **Descrição**: manter `docs/queries.sql` com a consulta `ST_DWithin` + `ST_Distance` e consultas de apoio (`EXPLAIN ANALYZE`, fila de candidatos).
- **Critério de aceite**: roda no psql com `\set loteId N` e o plano usa o índice GIST.
- **Responsável**: Luccas · `banco` `documentação`

## Épico 2 – API (Spring Boot)

### 4. Autenticação JWT e perfis
- **Descrição**: login com e-mail e senha (BCrypt), JWT com expiração e autorização por perfil (comerciante, receptor, voluntário).
- **Critério de aceite**: rota protegida sem token devolve 401; perfil errado devolve 403; senha guardada como hash BCrypt.
- **Responsável**: Luccas · `back-end`

### 5. Publicação de lote com foto
- **Descrição**: `POST /api/lotes` (multipart) com foto, categoria, quantidade, unidade, peso, "retirar até" e confirmação de validade.
- **Critério de aceite**: rejeita refeições prontas e vencidos; refrigerados com janela > 2 h são recusados; sem o checkbox, erro 400; a confirmação fica gravada no lote.
- **Responsável**: Luccas · `back-end`

### 6. Matching geográfico e fila de candidatos
- **Descrição**: ao publicar, selecionar receptores a até 2 km que aceitam a categoria, gravar a ordem em `lote_candidato` e avisar cada um.
- **Critério de aceite**: testes cobrem dentro/fora do raio e categoria aceita/não aceita; tempo < 500 ms.
- **Responsável**: Luccas · `back-end` `teste`

### 7. Reserva sem reserva dupla
- **Descrição**: `POST /api/lotes/{id}/reservas` com `SELECT ... FOR UPDATE` e `@Version`; código de 4 dígitos + conteúdo do QR; validade configurável (`app.reserva.minutos`, 45 min; 1 min no perfil `demo`).
- **Critério de aceite**: duas reservas simultâneas → uma 201 e uma 409; só uma reserva ATIVA no banco.
- **Responsável**: Luccas · `back-end` `teste`

### 8. Expiração com `@Scheduled`
- **Descrição**: reservas vencidas voltam o lote para DISPONIVEL e avisam o próximo candidato (excluindo quem deixou expirar); lotes que passam do "retirar até" viram EXPIRADO.
- **Critério de aceite**: no perfil `demo`, 1 min depois da reserva da Mãos Dadas o lote está DISPONIVEL e a Comunidade Esperança recebeu o aviso.
- **Responsável**: Luccas · `back-end` `teste`

### 9. Confirmação de retirada e recibo digital
- **Descrição**: `POST /api/retiradas` por código (QR ou 4 dígitos); lote RETIRADO e recibo com data/hora, comércio, item, quantidade, peso, receptor, declaração de validade e o texto "Doação realizada de boa-fé, nos termos da Lei nº 14.016/2020."
- **Critério de aceite**: código de outro comércio é recusado; o recibo não menciona benefício fiscal.
- **Responsável**: Luccas · `back-end`

### 10. Rota do voluntário
- **Descrição**: `GET /api/rota` com vizinho mais próximo a partir do voluntário, paradas nos comércios e fim no receptor; distâncias por trecho, total, tempo por modal (5/15/25 km/h) e link do Google Maps sem chave.
- **Critério de aceite**: com os lotes da padaria e da quitanda reservados pela Mãos Dadas, a rota da Ana é Padaria → Quitanda → Mãos Dadas.
- **Responsável**: Luccas · `back-end` `teste`

### 11. Painel de impacto e cartaz do dia
- **Descrição**: `GET /api/impacto/geral` e `/meu` (kg, refeições = kg ÷ 0,54, CO₂e = kg × 0,627, lotes, % retirado, tempo médio) e `GET /api/cartaz`.
- **Critério de aceite**: 4 kg retirados → 7 refeições e 2,5 kg CO₂e (teste unitário).
- **Responsável**: Luccas · `back-end` `teste`

### 12. Push FCM com fallback
- **Descrição**: enviar push pelo Firebase Cloud Messaging quando `FIREBASE_CREDENTIALS` existir; sempre gravar o aviso em `notificacao` para o polling do app.
- **Critério de aceite**: sem credenciais a API sobe, registra o log e os avisos aparecem no app em até 15 s.
- **Responsável**: Luccas · `back-end`

### 13. Swagger e tratamento de erros
- **Descrição**: springdoc-openapi com autenticação Bearer; erros em `ProblemDetail` com mensagens em português.
- **Critério de aceite**: `/swagger-ui.html` lista todos os endpoints e permite testar com o token.
- **Responsável**: Luccas · `back-end` `documentação`

## Épico 3 – PWA (React)

### 14. Identidade visual e logo em SVG
- **Descrição**: variáveis CSS da paleta, Inter 800 nos títulos, Nunito no corpo, eyebrow e pill; logo (rota tracejada + alvo âmbar) em SVG; ícones PWA 192/512 maskable e favicon.
- **Critério de aceite**: telas conferidas com a capa e o slide; contraste AA (axe-core sem violações).
- **Responsável**: Gabrielly · `front-end`

### 15. Abertura/login com "Entrar como"
- **Descrição**: tela com fundo verde-noite, rota decorativa terminando no alvo âmbar e o slogan; botões para os usuários de demonstração.
- **Critério de aceite**: um toque entra com cada perfil e cai na tela inicial correta.
- **Responsável**: Gabrielly · `front-end`

### 16. Comerciante: início e publicar lote em 30 s
- **Descrição**: lista dos lotes de hoje, atalho "Publicar sobra", formulário com foto comprimida no navegador, categorias (não aceitas desabilitadas com explicação), peso pré-preenchido, "retirar até" e checkbox obrigatório.
- **Critério de aceite**: publicar 20 pães leva menos de 30 s e mostra as organizações avisadas.
- **Responsável**: Gabrielly · `front-end`

### 17. Comerciante: confirmar retirada, recibos, cartaz e impacto
- **Descrição**: leitura do QR pela câmera (html5-qrcode) ou digitação do código; recibo e cartaz A4 imprimíveis; painel com rodapé de fontes.
- **Critério de aceite**: cartaz e recibo saem em uma página A4 no "Imprimir/Salvar como PDF".
- **Responsável**: Gabrielly · `front-end`

### 18. Receptor: lotes próximos, reserva e WhatsApp
- **Descrição**: lista por distância com foto, categoria, quantidade, prazo e alerta de refrigerado; reserva com contagem regressiva, QR + código e botão "Compartilhar no WhatsApp" (`wa.me`).
- **Critério de aceite**: a mensagem abre pronta no WhatsApp com o nome e endereço da ONG e o horário.
- **Responsável**: Gabrielly · `front-end`

### 19. Voluntário: minha rota
- **Descrição**: paradas em ordem com distância por trecho, total, tempo estimado, QR de cada coleta e link do Google Maps.
- **Critério de aceite**: a rota da Ana mostra as duas paradas e termina na Associação Mãos Dadas.
- **Responsável**: Gabrielly · `front-end`

### 20. PWA instalável, offline do app shell e avisos
- **Descrição**: manifest, service worker com precache, push web (quando configurado) e toast de avisos por polling a cada 15 s.
- **Critério de aceite**: Lighthouse reconhece como instalável; bundle inicial ≤ 300 KB gzip (`npm run tamanho`).
- **Responsável**: Gabrielly · `front-end`

### 21. Tela "Como funciona"
- **Descrição**: passos do fluxo, explicação da Lei nº 14.016/2020 e do uso de dados (LGPD).
- **Critério de aceite**: acessível sem login e pela navegação do receptor e do voluntário.
- **Responsável**: Gabrielly (texto revisado por Victor) · `front-end` `pesquisa`

## Épico 4 – Infra, qualidade e documentação

### 22. Deploy na Azure com GitHub Actions
- **Descrição**: workflows para Static Web Apps (front) e App Service (API); PostgreSQL Flexible Server com `azure.extensions = POSTGIS`.
- **Critério de aceite**: push na `main` publica front e API; a API sobe aplicando as migrações no banco da Azure.
- **Responsável**: Luccas (API e banco) e Gabrielly (front) · `infra`

### 23. Testes automatizados
- **Descrição**: JUnit 5 + Mockito para matching, expiração, reserva dupla, painel e rota; testes contra PostGIS real no CI.
- **Critério de aceite**: `mvn test` verde localmente e no GitHub Actions.
- **Responsável**: Luccas · `teste`

### 24. README, arquitetura e requisitos não funcionais
- **Descrição**: passo a passo local, variáveis de ambiente, usuários de demonstração, perfil demo, deploy; diagrama Mermaid e justificativa PaaS; RNFs com evidências.
- **Critério de aceite**: alguém de fora da equipe sobe o projeto só seguindo o README.
- **Responsável**: Luccas e Gabrielly · `documentação`

## Épico 5 – Pesquisa, pitch e vídeo

### 25. Pesquisa de campo com comércios e ONGs do bairro
- **Descrição**: conversar com ao menos 3 comércios e 2 organizações sobre horários de sobra, medo de responsabilização e como avisam as famílias hoje.
- **Critério de aceite**: resumo com citações e números para o slide "O problema".
- **Responsável**: Victor · `pesquisa`

### 26. Fontes dos indicadores de impacto
- **Descrição**: documentar as fontes dos fatores (0,54 kg por refeição – Feeding America/USDA; 0,627 kg CO₂e por kg – fatores do governo do Reino Unido, BEIS/DESNZ) e da Lei nº 14.016/2020.
- **Critério de aceite**: links e trechos citados no roteiro do pitch e no rodapé do painel.
- **Responsável**: Victor · `pesquisa`

### 27. Roteiro e gravação do vídeo de pitch
- **Descrição**: roteiro seguindo a seção "Roteiro da demonstração" do README (publicar → matching no banco → reserva e WhatsApp → expiração no perfil demo → rota → retirada e painel).
- **Critério de aceite**: vídeo dentro do tempo do edital, mostrando os 5 passos sem cortes de erro.
- **Responsável**: Victor (com Gabrielly na operação do app) · `pitch`

### 28. Slides do pitch na identidade visual
- **Descrição**: capa, problema, solução, demonstração, impacto, arquitetura PaaS e equipe, usando a paleta e a tipografia do projeto.
- **Critério de aceite**: slides revisados pela equipe e alinhados ao vídeo.
- **Responsável**: Victor · `pitch`
