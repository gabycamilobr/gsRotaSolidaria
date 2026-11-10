# Requisitos não funcionais – Rota Solidária

Cada requisito traz **como foi implementado** e **como conferir**. Os números abaixo foram medidos no protótipo em 10/11/2026.

## 1. Desempenho em rede lenta

| Meta | Implementação | Como conferir |
|---|---|---|
| Bundle inicial do front ≤ 300 KB gzip | Telas de cada perfil carregadas sob demanda (`React.lazy`); leitor de QR (`html5-qrcode`), gerador de QR e Firebase em chunks separados, baixados só quando usados | `cd frontend && npm run build && npm run tamanho` → **≈ 94 KB gzip** (HTML + JS + CSS iniciais). O script falha se passar de 300 KB |
| Imagens comprimidas | Foto redimensionada no navegador (lado maior 1024 px, JPEG qualidade 0,7) antes do upload (`src/lib/imagem.js`); limite de 2 MB validado na API | Ao publicar, a tela mostra o tamanho final (ex.: foto de 600 KB → ~70 KB) |
| Cache do app shell no service worker | `vite-plugin-pwa` (estratégia `injectManifest`, `src/sw.js`): HTML, CSS, JS das telas e ícones em precache; fontes, fotos dos lotes e chunks sob demanda em cache em tempo de execução | DevTools → Application → Service Workers / Cache Storage |
| Funcionar razoavelmente em "Slow 3G" | Itens acima + fontes com `display=swap`, fundo pintado antes do CSS, compressão gzip das respostas da API (`server.compression`) | Perfil Slow 3G do DevTools (2 s de latência, 400 kbps): **1ª visita utilizável em ~8,8 s; visitas seguintes em ~0,5 s** (app shell vindo do service worker); login + início + tela de publicar em ~2,4 s |

## 2. Mobile-first

- Layout pensado para **360 px** de largura (conteúdo com no máximo 640 px em telas maiores), navegação inferior fixa e cabeçalho compacto.
- **Áreas de toque ≥ 44 px**: botões e campos com altura mínima de 48 px (`--toque`), itens da navegação com 60 px, botão "Publicar sobra" com 88 px.
- Entrada de foto com `capture="environment"` (abre a câmera traseira), teclado numérico (`inputMode`) para quantidade, peso e código.
- Todas as telas do roteiro foram percorridas a 360 × 760 px com Playwright, sem rolagem horizontal.

## 3. Acessibilidade

- **Contraste WCAG AA**: paleta conferida (âmbar sobre verde-noite ≈ 7:1; `--ambar-texto` #9A5B16 sobre creme ≈ 4,7:1, usado em textos pequenos no claro). O wordmark "Solidária" em âmbar é parte do logo (isento pelo critério 1.4.3) e é desenhado em SVG.
- **Labels em todos os campos**, mensagens de erro ligadas aos campos (`aria-invalid`), grupos com `fieldset`/`legend`.
- **Navegação por teclado**: link "Pular para o conteúdo", foco visível âmbar (`:focus-visible`), painel de avisos fecha com Esc, sem armadilhas de foco.
- **Textos alternativos** nas fotos dos lotes, QR code com `aria-label` que lê o código dígito a dígito, ícones decorativos com `aria-hidden`.
- Avisos novos anunciados por leitor de tela (`aria-live="polite"`), contagem regressiva com `role="timer"`, respeito a `prefers-reduced-motion`.
- **Verificação automática**: axe-core (regras WCAG 2.0/2.1 A e AA) nas 14 telas do protótipo, **sem violações**.

## 4. LGPD

- **Dados mínimos**: comércio/receptor (nome, endereço, coordenadas, e-mail de login), voluntário (nome, organização, modal, ponto de partida). Nenhum CPF, telefone ou documento.
- **Localização** apenas de estabelecimentos, receptores e voluntários, com **consentimento registrado no cadastro** (`usuario.consentimento_localizacao_em`). A localização atual do voluntário só é usada se ele pedir, para calcular a rota, e **não é gravada**.
- **Nenhum dado de família**: as famílias não se cadastram; a ONG fala com elas pelo WhatsApp dela (link `wa.me`), e a resposta vai para a ONG, nunca para a plataforma.
- Fotos só do alimento. A tela "Como funciona" explica o uso de dados em linguagem simples.

## 5. Segurança

- **Senhas com BCrypt** (`BCryptPasswordEncoder`).
- **JWT com expiração** (12 h por padrão, `JWT_EXPIRACAO_HORAS`), assinado com HS256; segredo vindo de `JWT_SECRET` (mínimo 32 caracteres; a API recusa subir com segredo curto).
- **Autorização por perfil** (`@PreAuthorize` com `ROLE_COMERCIANTE`, `ROLE_RECEPTOR`, `ROLE_VOLUNTARIO`) e checagem de dono do recurso (um comércio não confirma retirada de outro; um receptor fora do raio não vê nem reserva o lote).
- **Validação de entrada no back-end** com Bean Validation (`@NotNull`, `@DecimalMin`, `@Future`, `@AssertTrue`, `@Pattern`…) e regras de negócio (categoria aceita, janela de 2 h para refrigerados, tipo e tamanho da foto). Erros devolvidos como `ProblemDetail` (RFC 7807) em português.
- **CORS restrito ao domínio do front** (`CORS_ORIGINS`).
- Concorrência: `SELECT ... FOR UPDATE` + `@Version` + índice único parcial impedem reserva dupla (testado com duas reservas simultâneas contra o PostgreSQL real).

## 6. Disponibilidade e manutenção

- Arquitetura **100 % PaaS na Azure** (Static Web Apps, App Service, PostgreSQL Flexible Server): sem VM para administrar, com backup automático do banco e escala por configuração. Ver [arquitetura.md](arquitetura.md).
- **Migrações versionadas** com Flyway (`backend/src/main/resources/db/migration`); o Hibernate só valida o esquema (`ddl-auto: validate`).
- Health check em `/actuator/health` para o App Service.
- Deploy automatizado por GitHub Actions (`.github/workflows`), com os testes rodando antes do deploy da API.
- Push desacoplado: sem Firebase, o sistema segue funcionando com avisos dentro do app.

## 7. Tempo de resposta do matching < 500 ms

- Índices **GIST** em todas as colunas `geography` (`comercio`, `receptor`, `voluntario`, `lote`).
- `EXPLAIN ANALYZE` da consulta de [queries.sql](queries.sql) usa `Index Scan using receptor_localizacao_gix` e executa em **menos de 1 ms** com os dados de demonstração.
- A API registra o tempo do matching em log (`Matching do lote N: X receptor(es) em Y ms`) e o teste `MatchingConsultaPostgisTest.desempenho` garante < 500 ms.
