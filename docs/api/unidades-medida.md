# API — Unidades de Medida

Base: `/api/v1/unidades-medida`

Módulo de cadastro das unidades de medida usadas pelo estoque da Farmácia Viva. Nesta fase o módulo está temporariamente liberado sem autenticação; quando o módulo de usuários com JWT for implementado, todos os endpoints passarão a exigir o header `Authorization: Bearer <token>`.

## Modelo

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | UUID | Identificador gerado pelo sistema |
| `codigo` | string (até 20) | Código único, sem distinção de maiúsculas e minúsculas; sempre armazenado e retornado em maiúsculas |
| `nome` | string (até 100) | Nome da unidade |
| `dimensao` | enum | `MASSA`, `VOLUME` ou `CONTAGEM` |
| `fatorParaBase` | decimal (13 inteiros, 6 decimais) | Fator de conversão para a unidade base da dimensão; deve ser maior que zero |
| `unidadeBase` | boolean | Indica se esta é a unidade base da sua dimensão |
| `casasDecimais` | inteiro | Casas decimais permitidas nas quantidades; entre 0 e 6 |
| `ativo` | boolean | `false` indica unidade arquivada (arquivamento lógico; não há exclusão física) |
| `criadoEm` | date-time (ISO 8601, UTC) | Data de criação |
| `atualizadoEm` | date-time (ISO 8601, UTC) | Data da última atualização |

Unidades arquivadas continuam visíveis: `GET /{id}` sempre retorna a unidade, e a listagem retorna arquivadas quando filtrada com `ativo=false` ou quando nenhum filtro é informado. Isso garante que o histórico de movimentações continue legível.

## Formato de erro

Todos os erros seguem o formato:

```json
{
  "timestamp": "2026-08-04T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Erro de validacao nos dados enviados",
  "path": "/api/v1/unidades-medida",
  "fieldErrors": [
    { "field": "codigo", "message": "codigo e obrigatorio" }
  ]
}
```

`fieldErrors` só é preenchido em erros de validação (HTTP 400).

Códigos de erro usados pelo módulo:

| Código | Situação |
|---|---|
| 400 | Corpo inválido, parâmetro com tipo inválido (`ativo=talvez`), campo de ordenação inexistente (`sort=campoInexistente`) |
| 404 | Id inexistente |
| 405 | Método não suportado no recurso (por exemplo `DELETE`, já que não há exclusão física) |
| 409 | Código já utilizado por outra unidade, ignorando maiúsculas e minúsculas |
| 415 | `Content-Type` diferente de `application/json` |
| 422 | Violação de regra de negócio validada no service (fator não positivo, fator com mais de 6 casas decimais, `casasDecimais` fora de 0 a 6) |
| 500 | Erro inesperado |

O `fatorParaBase` é armazenado com escala fixa de 6 casas decimais, igual à da coluna `NUMERIC(19,6)`. Um fator com mais de 6 casas decimais é rejeitado em vez de ser arredondado silenciosamente pelo banco. Por isso as respostas sempre apresentam o fator com 6 casas (`1.000000`), tanto logo após a criação quanto em consultas posteriores.

## POST /api/v1/unidades-medida

Cria uma unidade de medida. O `codigo` é normalizado para maiúsculas antes de salvar.

Corpo da requisição:

```json
{
  "codigo": "mg",
  "nome": "Miligrama",
  "dimensao": "MASSA",
  "fatorParaBase": 0.001,
  "unidadeBase": false,
  "casasDecimais": 6
}
```

Respostas:

`201 Created` com header `Location: /api/v1/unidades-medida/{id}` e corpo:

```json
{
  "id": "3f0b1a2c-9c1d-4b8e-8a5f-2f6f0f1a2b3c",
  "codigo": "MG",
  "nome": "Miligrama",
  "dimensao": "MASSA",
  "fatorParaBase": 0.001,
  "unidadeBase": false,
  "casasDecimais": 6,
  "ativo": true,
  "criadoEm": "2026-08-04T12:00:00Z",
  "atualizadoEm": "2026-08-04T12:00:00Z"
}
```

`400 Bad Request` para violações de validação (código ou nome ausentes, `fatorParaBase` menor ou igual a zero, `casasDecimais` fora de 0 a 6, `dimensao` inválida).

`409 Conflict` quando já existe unidade com o mesmo código, ignorando maiúsculas e minúsculas.

## GET /api/v1/unidades-medida

Lista unidades com paginação e filtro opcional por situação.

Parâmetros de consulta:

| Parâmetro | Tipo | Padrão | Descrição |
|---|---|---|---|
| `ativo` | boolean | (sem filtro) | `true` retorna só ativas; `false` retorna só arquivadas; ausente retorna todas |
| `page` | inteiro | 0 | Página, iniciando em 0 |
| `size` | inteiro | 20 | Tamanho da página |
| `sort` | string | `codigo,asc` | Ordenação no formato `campo,direcao`; campo inexistente resulta em 400 |

O tamanho máximo de página é 100. Valores maiores são reduzidos a esse limite.

Resposta `200 OK`:

```json
{
  "content": [
    {
      "id": "3f0b1a2c-9c1d-4b8e-8a5f-2f6f0f1a2b3c",
      "codigo": "G",
      "nome": "Grama",
      "dimensao": "MASSA",
      "fatorParaBase": 1.0,
      "unidadeBase": true,
      "casasDecimais": 3,
      "ativo": true,
      "criadoEm": "2026-08-04T12:00:00Z",
      "atualizadoEm": "2026-08-04T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

## GET /api/v1/unidades-medida/{id}

Busca uma unidade pelo id, incluindo unidades arquivadas.

Respostas: `200 OK` com o corpo da unidade; `404 Not Found` se o id não existir; `400 Bad Request` se o id não for um UUID válido.

## PUT /api/v1/unidades-medida/{id}

Atualiza `codigo`, `nome`, `dimensao`, `fatorParaBase`, `unidadeBase` e `casasDecimais`. Não altera `ativo` (use os endpoints de arquivar e ativar) nem as datas de auditoria, que são gerenciadas pelo sistema.

Corpo da requisição: mesmo formato do POST.

Respostas: `200 OK` com a unidade atualizada; `400 Bad Request` para violações de validação; `404 Not Found` se o id não existir; `409 Conflict` se o novo código já pertencer a outra unidade, ignorando maiúsculas e minúsculas; `422 Unprocessable Entity` para violações de regra de negócio.

O campo `atualizadoEm` já vem atualizado na própria resposta do PUT, e `criadoEm` permanece inalterado.

## PATCH /api/v1/unidades-medida/{id}/arquivar

Arquiva a unidade (arquivamento lógico): altera `ativo` para `false` sem excluir o registro. A operação é idempotente; arquivar uma unidade já arquivada mantém `ativo` como `false`. A unidade permanece visível por id e nas listagens, preservando o histórico.

Respostas: `200 OK` com a unidade arquivada; `404 Not Found` se o id não existir.

## PATCH /api/v1/unidades-medida/{id}/ativar

Reativa uma unidade arquivada: altera `ativo` para `true`. A operação é idempotente.

Respostas: `200 OK` com a unidade reativada; `404 Not Found` se o id não existir.

## Exemplos com curl

```bash
# Criar
curl -X POST http://localhost:8080/api/v1/unidades-medida \
  -H "Content-Type: application/json" \
  -d '{"codigo":"g","nome":"Grama","dimensao":"MASSA","fatorParaBase":1.0,"unidadeBase":true,"casasDecimais":3}'

# Listar ativas, primeira página com 10 registros
curl "http://localhost:8080/api/v1/unidades-medida?ativo=true&page=0&size=10&sort=codigo,asc"

# Buscar por id
curl http://localhost:8080/api/v1/unidades-medida/3f0b1a2c-9c1d-4b8e-8a5f-2f6f0f1a2b3c

# Atualizar
curl -X PUT http://localhost:8080/api/v1/unidades-medida/3f0b1a2c-9c1d-4b8e-8a5f-2f6f0f1a2b3c \
  -H "Content-Type: application/json" \
  -d '{"codigo":"G","nome":"Grama","dimensao":"MASSA","fatorParaBase":1.0,"unidadeBase":true,"casasDecimais":3}'

# Arquivar
curl -X PATCH http://localhost:8080/api/v1/unidades-medida/3f0b1a2c-9c1d-4b8e-8a5f-2f6f0f1a2b3c/arquivar

# Reativar
curl -X PATCH http://localhost:8080/api/v1/unidades-medida/3f0b1a2c-9c1d-4b8e-8a5f-2f6f0f1a2b3c/ativar
```
