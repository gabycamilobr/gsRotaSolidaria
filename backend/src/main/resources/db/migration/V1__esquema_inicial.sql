-- Rota Solidária – esquema inicial
-- Na Azure (PostgreSQL Flexible Server) o PostGIS precisa estar liberado no
-- parâmetro de servidor azure.extensions ANTES deste CREATE EXTENSION.
CREATE EXTENSION IF NOT EXISTS postgis;

-- Estabelecimentos que doam. A localização é guardada como latitude/longitude
-- (mapeadas pelo JPA) e a coluna geography é gerada pelo próprio banco.
CREATE TABLE comercio (
    id            BIGSERIAL PRIMARY KEY,
    nome          VARCHAR(120) NOT NULL,
    endereco      VARCHAR(200) NOT NULL,
    latitude      DOUBLE PRECISION NOT NULL,
    longitude     DOUBLE PRECISION NOT NULL,
    localizacao   geography(Point, 4326)
                  GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography) STORED,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ONGs, igrejas e associações de moradores. O endereço é o ponto de distribuição.
CREATE TABLE receptor (
    id                  BIGSERIAL PRIMARY KEY,
    nome                VARCHAR(120) NOT NULL,
    endereco            VARCHAR(200) NOT NULL,
    latitude            DOUBLE PRECISION NOT NULL,
    longitude           DOUBLE PRECISION NOT NULL,
    localizacao         geography(Point, 4326)
                        GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography) STORED,
    categorias_aceitas  VARCHAR(30)[] NOT NULL,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE voluntario (
    id            BIGSERIAL PRIMARY KEY,
    nome          VARCHAR(120) NOT NULL,
    receptor_id   BIGINT NOT NULL REFERENCES receptor(id),
    modal         VARCHAR(20) NOT NULL CHECK (modal IN ('A_PE', 'BICICLETA', 'CARRO')),
    latitude      DOUBLE PRECISION NOT NULL,
    longitude     DOUBLE PRECISION NOT NULL,
    localizacao   geography(Point, 4326)
                  GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography) STORED,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Login. Cada usuário pertence a exatamente um perfil e aponta para a entidade dele.
CREATE TABLE usuario (
    id                          BIGSERIAL PRIMARY KEY,
    email                       VARCHAR(120) NOT NULL UNIQUE,
    senha_hash                  VARCHAR(100) NOT NULL,
    perfil                      VARCHAR(20) NOT NULL CHECK (perfil IN ('COMERCIANTE', 'RECEPTOR', 'VOLUNTARIO')),
    comercio_id                 BIGINT REFERENCES comercio(id),
    receptor_id                 BIGINT REFERENCES receptor(id),
    voluntario_id               BIGINT REFERENCES voluntario(id),
    consentimento_localizacao_em TIMESTAMPTZ NOT NULL,
    push_token                  VARCHAR(512),
    criado_em                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT usuario_entidade_ck CHECK (
        (perfil = 'COMERCIANTE' AND comercio_id IS NOT NULL) OR
        (perfil = 'RECEPTOR'    AND receptor_id IS NOT NULL) OR
        (perfil = 'VOLUNTARIO'  AND voluntario_id IS NOT NULL)
    )
);

CREATE TABLE lote (
    id                     BIGSERIAL PRIMARY KEY,
    comercio_id            BIGINT NOT NULL REFERENCES comercio(id),
    categoria              VARCHAR(30) NOT NULL,
    descricao              VARCHAR(80) NOT NULL,
    quantidade             NUMERIC(10, 2) NOT NULL CHECK (quantidade > 0),
    unidade                VARCHAR(20) NOT NULL CHECK (unidade IN ('UNIDADES', 'KG', 'BANDEJAS')),
    peso_kg                NUMERIC(10, 2) NOT NULL CHECK (peso_kg > 0),
    retirar_ate            TIMESTAMPTZ NOT NULL,
    confirmacao_validade   BOOLEAN NOT NULL CHECK (confirmacao_validade),
    confirmado_em          TIMESTAMPTZ NOT NULL,
    status                 VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL'
                           CHECK (status IN ('DISPONIVEL', 'RESERVADO', 'RETIRADO', 'EXPIRADO', 'CANCELADO')),
    -- cópia da posição do comércio no momento da publicação (usada no matching)
    latitude               DOUBLE PRECISION NOT NULL,
    longitude              DOUBLE PRECISION NOT NULL,
    localizacao            geography(Point, 4326)
                           GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography) STORED,
    publicado_em           TIMESTAMPTZ NOT NULL DEFAULT now(),
    retirado_em            TIMESTAMPTZ,
    version                BIGINT NOT NULL DEFAULT 0
);

-- Foto do lote, comprimida no navegador (lado maior 1024 px, JPEG 0,7). Fica numa
-- tabela separada para que as listagens de lotes não carreguem os bytes.
CREATE TABLE lote_foto (
    lote_id   BIGINT PRIMARY KEY REFERENCES lote(id) ON DELETE CASCADE,
    conteudo  BYTEA NOT NULL,
    tipo      VARCHAR(30) NOT NULL
);

-- Receptores selecionados pelo matching, na ordem de distância.
CREATE TABLE lote_candidato (
    lote_id        BIGINT NOT NULL REFERENCES lote(id) ON DELETE CASCADE,
    receptor_id    BIGINT NOT NULL REFERENCES receptor(id),
    ordem          INT NOT NULL,
    distancia_m    INT NOT NULL,
    notificado_em  TIMESTAMPTZ,
    PRIMARY KEY (lote_id, receptor_id)
);

CREATE TABLE reserva (
    id            BIGSERIAL PRIMARY KEY,
    lote_id       BIGINT NOT NULL REFERENCES lote(id),
    receptor_id   BIGINT NOT NULL REFERENCES receptor(id),
    codigo        VARCHAR(4) NOT NULL,
    status        VARCHAR(20) NOT NULL CHECK (status IN ('ATIVA', 'EXPIRADA', 'CONCLUIDA', 'CANCELADA')),
    criada_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_em     TIMESTAMPTZ NOT NULL,
    encerrada_em  TIMESTAMPTZ
);
-- garante, no banco, no máximo uma reserva ATIVA por lote (segunda linha de defesa contra reserva dupla)
CREATE UNIQUE INDEX reserva_ativa_por_lote_uk ON reserva (lote_id) WHERE status = 'ATIVA';

CREATE TABLE recibo (
    id                    BIGSERIAL PRIMARY KEY,
    lote_id               BIGINT NOT NULL UNIQUE REFERENCES lote(id),
    numero                VARCHAR(20) NOT NULL UNIQUE,
    emitido_em            TIMESTAMPTZ NOT NULL,
    comercio_nome         VARCHAR(120) NOT NULL,
    comercio_endereco     VARCHAR(200) NOT NULL,
    item                  VARCHAR(80) NOT NULL,
    categoria             VARCHAR(30) NOT NULL,
    quantidade            NUMERIC(10, 2) NOT NULL,
    unidade               VARCHAR(20) NOT NULL,
    peso_kg               NUMERIC(10, 2) NOT NULL,
    receptor_nome         VARCHAR(120) NOT NULL,
    receptor_endereco     VARCHAR(200) NOT NULL,
    confirmacao_validade  VARCHAR(200) NOT NULL,
    confirmado_em         TIMESTAMPTZ NOT NULL,
    texto_legal           VARCHAR(200) NOT NULL
);

-- Avisos dentro do app (sempre gravados; o push do FCM é um canal adicional).
CREATE TABLE notificacao (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT NOT NULL REFERENCES usuario(id),
    titulo      VARCHAR(120) NOT NULL,
    mensagem    VARCHAR(400) NOT NULL,
    lote_id     BIGINT REFERENCES lote(id),
    criada_em   TIMESTAMPTZ NOT NULL DEFAULT now(),
    lida        BOOLEAN NOT NULL DEFAULT FALSE
);

-- Índices GIST para o matching geográfico (ST_DWithin / ST_Distance) abaixo de 500 ms.
CREATE INDEX comercio_localizacao_gix   ON comercio   USING GIST (localizacao);
CREATE INDEX receptor_localizacao_gix   ON receptor   USING GIST (localizacao);
CREATE INDEX voluntario_localizacao_gix ON voluntario USING GIST (localizacao);
CREATE INDEX lote_localizacao_gix       ON lote       USING GIST (localizacao);

CREATE INDEX lote_status_idx           ON lote (status);
CREATE INDEX lote_comercio_idx         ON lote (comercio_id, publicado_em DESC);
CREATE INDEX lote_candidato_receptor   ON lote_candidato (receptor_id);
CREATE INDEX reserva_status_expira_idx ON reserva (status, expira_em);
CREATE INDEX notificacao_usuario_idx   ON notificacao (usuario_id, id);
