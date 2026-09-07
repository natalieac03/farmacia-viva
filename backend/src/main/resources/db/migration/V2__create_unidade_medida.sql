-- V2: tabela de unidades de medida.
-- Regras refletidas no schema:
--   - codigo unico ignorando maiusculas e minusculas (indice unico em UPPER(codigo));
--   - fator_para_base maior que zero;
--   - casas_decimais entre 0 e 6;
--   - exclusao fisica nao e utilizada: o arquivamento logico usa a coluna ativo.

CREATE TABLE unidade_medida (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo          VARCHAR(20)   NOT NULL,
    nome            VARCHAR(100)  NOT NULL,
    dimensao        VARCHAR(20)   NOT NULL,
    fator_para_base NUMERIC(19,6) NOT NULL,
    unidade_base    BOOLEAN       NOT NULL DEFAULT FALSE,
    casas_decimais  INTEGER       NOT NULL,
    ativo           BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMPTZ   NOT NULL,
    atualizado_em   TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_unidade_medida_dimensao
        CHECK (dimensao IN ('MASSA', 'VOLUME', 'CONTAGEM')),
    CONSTRAINT ck_unidade_medida_fator_positivo
        CHECK (fator_para_base > 0),
    CONSTRAINT ck_unidade_medida_casas_decimais
        CHECK (casas_decimais BETWEEN 0 AND 6)
);

CREATE UNIQUE INDEX ux_unidade_medida_codigo
    ON unidade_medida (UPPER(codigo));

CREATE INDEX ix_unidade_medida_ativo
    ON unidade_medida (ativo);
