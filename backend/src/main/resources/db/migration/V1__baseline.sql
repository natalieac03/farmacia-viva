-- V1: baseline do banco de dados da Farmacia Viva (CREMIC).
-- Habilita a extensao pgcrypto, que fornece a funcao gen_random_uuid()
-- usada como valor padrao das chaves primarias UUID nas proximas migrations.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
