-- V3: repositorio de plantas.
--
-- Quatro tabelas: a ficha da planta, seus nomes populares, os vinculos de uso
-- similar entre plantas (autorrelacionamento) e as referencias bibliograficas.
--
-- VINCULO COM O ESTOQUE (intencional, ainda nao criado)
-- ------------------------------------------------------
-- A intencao e que cada Item de estoque aponte para uma planta
-- (item.planta_id uuid nullable, FK para planta), e que o nome cientifico do
-- Item passe a ser derivado da planta em vez de duplicado no cadastro do item.
-- A tabela item NAO EXISTE neste repositorio ainda, portanto esta migration
-- nao cria a coluna nem a FK. Quando o modulo de Item for construido, ele
-- ganha a coluna e a constraint em migration propria, aditiva.
--
-- Regras refletidas no schema:
--   - o nome cientifico e a identidade da planta: indice unico na forma
--     normalizada (sem acentos, maiusculas, espacos colapsados, pontuacao
--     normalizada), para que "Mikania glomerata  Spreng." e
--     "MIKANIA GLOMERATA SPRENG." colidam no banco, nao so no Java;
--   - a mesma planta nao repete um nome popular; plantas diferentes PODEM
--     compartilhar nome popular ("erva-cidreira" nomeia varias especies);
--   - similaridade e armazenada DIRECIONADA, uma linha por sentido; o service
--     cria o par reciproco ao vincular e remove os dois ao desvincular;
--   - referencia precisa de titulo OU texto livre;
--   - exclusao fisica nao e utilizada: o arquivamento logico usa a coluna ativo.

CREATE TABLE planta (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome_cientifico             VARCHAR(200) NOT NULL,
    nome_cientifico_normalizado VARCHAR(200) NOT NULL,
    familia_botanica            VARCHAR(120),
    cultivo                     TEXT,
    indicacao_uso               TEXT,
    observacoes                 TEXT,
    ativo                       BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em                   TIMESTAMPTZ  NOT NULL,
    atualizado_em               TIMESTAMPTZ  NOT NULL,
    versao                      BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_planta_nome_cientifico_normalizado
    ON planta (nome_cientifico_normalizado);

CREATE INDEX ix_planta_ativo
    ON planta (ativo);

CREATE INDEX ix_planta_familia_botanica
    ON planta (familia_botanica);

CREATE TABLE planta_nome_popular (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    planta_id        UUID         NOT NULL,
    nome             VARCHAR(150) NOT NULL,
    nome_normalizado VARCHAR(150) NOT NULL,
    principal        BOOLEAN      NOT NULL DEFAULT FALSE,
    ordem            INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT fk_planta_nome_popular_planta
        FOREIGN KEY (planta_id) REFERENCES planta (id) ON DELETE CASCADE,
    CONSTRAINT ux_planta_nome_popular_planta_nome
        UNIQUE (planta_id, nome_normalizado)
);

-- A busca por nome popular e o caminho de entrada mais usado do repositorio.
CREATE INDEX ix_planta_nome_popular_nome_normalizado
    ON planta_nome_popular (nome_normalizado);

CREATE TABLE planta_similar (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    planta_id         UUID         NOT NULL,
    planta_similar_id UUID         NOT NULL,
    observacao        VARCHAR(500),
    criado_em         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_planta_similar_planta
        FOREIGN KEY (planta_id) REFERENCES planta (id),
    CONSTRAINT fk_planta_similar_planta_similar
        FOREIGN KEY (planta_similar_id) REFERENCES planta (id),
    CONSTRAINT ck_planta_similar_nao_reflexiva
        CHECK (planta_id <> planta_similar_id),
    CONSTRAINT ux_planta_similar_par
        UNIQUE (planta_id, planta_similar_id)
);

CREATE INDEX ix_planta_similar_planta_similar
    ON planta_similar (planta_similar_id);

CREATE TABLE planta_referencia (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    planta_id   UUID          NOT NULL,
    tipo        VARCHAR(40)   NOT NULL,
    autor       VARCHAR(250),
    titulo      VARCHAR(500),
    ano         INTEGER,
    link        VARCHAR(1000),
    texto_livre TEXT,
    ordem       INTEGER       NOT NULL DEFAULT 0,
    CONSTRAINT fk_planta_referencia_planta
        FOREIGN KEY (planta_id) REFERENCES planta (id) ON DELETE CASCADE,
    CONSTRAINT ck_planta_referencia_tipo
        CHECK (tipo IN ('LIVRO', 'ARTIGO', 'LEGISLACAO', 'SITE', 'OUTRO')),
    CONSTRAINT ck_planta_referencia_titulo_ou_texto
        CHECK (NULLIF(BTRIM(titulo), '') IS NOT NULL
            OR NULLIF(BTRIM(texto_livre), '') IS NOT NULL)
);

CREATE INDEX ix_planta_referencia_planta
    ON planta_referencia (planta_id);
