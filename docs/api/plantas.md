# API — Repositório de Plantas

Base: `/api/v1/plantas`

Fichas botânicas das plantas medicinais da Farmácia Viva: nome científico (a identidade da ficha), nomes populares, cultivo, indicação de uso, plantas de uso similar e referências bibliográficas. Nesta fase o módulo está temporariamente liberado sem autenticação; quando o módulo de usuários com JWT for implementado, todos os endpoints passarão a exigir `Authorization: Bearer <token>`.

## Modelo

### Planta (ficha completa — `PlantaResponse`)

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | UUID | Identificador gerado pelo sistema |
| `nomeCientifico` | string (até 200) | Obrigatório. Único na forma normalizada (sem acentos, maiúsculas, espaços colapsados, pontuação normalizada) |
| `familiaBotanica` | string (até 120), opcional | Família botânica |
| `cultivo` | texto, opcional | Como cultivar |
| `indicacaoUso` | texto, opcional | Indicação de uso |
| `observacoes` | texto, opcional | Observações livres |
| `ativo` | boolean | `false` indica ficha arquivada (arquivamento lógico; não há exclusão física) |
| `versao` | inteiro | Versão para bloqueio otimista |
| `criadoEm` / `atualizadoEm` | date-time (ISO 8601, UTC) | Auditoria |
| `nomesPopulares` | lista de `{ id, nome, principal, ordem }` | No máximo um `principal`; sem repetição após normalização |
| `referencias` | lista de `{ id, tipo, autor, titulo, ano, link, textoLivre, ordem }` | `tipo` ∈ `LIVRO`, `ARTIGO`, `LEGISLACAO`, `SITE`, `OUTRO`; exige `titulo` ou `textoLivre` |
| `similares` | lista de `{ id, nomeCientifico, nomePopularPrincipal, ativo, observacao }` | `id` é o id da OUTRA planta; `ativo=false` marca similar arquivada |

Campos nulos **não** aparecem no JSON (`default-property-inclusion: non_null`).

### Resumo (listagem — `PlantaResumoResponse`)

`{ id, nomeCientifico, familiaBotanica, nomesPopulares: string[], quantidadeSimilares, ativo }` — `nomesPopulares` vem com o principal primeiro.

### Opção (selects — `PlantaOpcaoResponse`)

`{ id, nomeCientifico, nomePopularPrincipal }` — apenas plantas ativas, ordenadas por nome científico.

### Corpo de criação e atualização (`PlantaRequest`)

```json
{
  "nomeCientifico": "Mikania glomerata Spreng.",
  "familiaBotanica": "Asteraceae",
  "cultivo": "Trepadeira perene propagada por estacas.",
  "indicacaoUso": "Expectorante.",
  "observacoes": null,
  "nomesPopulares": [
    { "nome": "Guaco", "principal": true },
    { "nome": "Guaco-liso", "principal": false }
  ],
  "referencias": [
    { "tipo": "LEGISLACAO", "autor": "Ministério da Saúde", "titulo": "RENISUS", "ano": 2009, "link": null, "textoLivre": null },
    { "tipo": "OUTRO", "autor": null, "titulo": null, "ano": null, "link": null, "textoLivre": "Anotação de campo." }
  ]
}
```

O mesmo corpo serve ao `POST` e ao `PUT`; no `PUT` as listas são substituídas por inteiro e a ordem das listas é a ordem exibida. Similares não entram aqui: vinculam-se pela ficha.

## Endpoints

| Método | Rota | Resposta | Descrição |
|---|---|---|---|
| `GET` | `/api/v1/plantas` | 200, página de resumos | Filtros: `busca` (nome científico OU nome popular, ignorando acento e caixa), `familiaBotanica` (contém, ignorando caixa), `ativo`; paginação `page`, `size` (máx. 100), `sort` (padrão `nomeCientifico,asc`) |
| `GET` | `/api/v1/plantas/opcoes` | 200, lista de opções | Para selects de outros módulos |
| `GET` | `/api/v1/plantas/{id}` | 200, ficha completa | Arquivadas continuam acessíveis |
| `POST` | `/api/v1/plantas` | 201 + `Location`, ficha | Cria |
| `PUT` | `/api/v1/plantas/{id}` | 200, ficha | Substitui a ficha inteira |
| `PATCH` | `/api/v1/plantas/{id}/arquivar` | 200, ficha | Arquivamento lógico; **preserva** os vínculos de similaridade |
| `PATCH` | `/api/v1/plantas/{id}/ativar` | 200, ficha | Reativa |
| `POST` | `/api/v1/plantas/{id}/similares` | 201, ficha | Corpo `{ "plantaSimilarId": "<uuid>", "observacao": "..." }`. Cria o vínculo **nos dois sentidos** |
| `DELETE` | `/api/v1/plantas/{id}/similares/{similarId}` | 204 | Remove o vínculo nos dois sentidos |

Não há `DELETE` de planta (responde 405).

## Similaridade

O vínculo é gravado direcionado, uma linha por sentido; o service cria o par recíproco ao vincular e remove os dois ao desvincular. Assim "similar a" vale nos dois sentidos sem `OR` na consulta. Regras: uma planta não se vincula a si mesma (422); plantas arquivadas não recebem nem viram vínculo (422); vínculo repetido responde 409.

## Códigos de erro

| Código | Situação |
|---|---|
| 400 | Corpo inválido (com `fieldErrors`), parâmetro com tipo inválido, campo de ordenação inexistente |
| 404 | Planta inexistente; vínculo de similaridade inexistente no `DELETE` |
| 405 | `DELETE` na planta |
| 409 | Nome científico já usado por outra planta (a mensagem informa qual: `...: <nome> (id <uuid>)`); vínculo de similaridade repetido |
| 415 | `Content-Type` diferente de `application/json` |
| 422 | Regras de negócio: referência sem título nem texto livre; nome popular repetido; mais de um nome popular principal; auto-vínculo; vínculo com planta arquivada |
| 500 | Erro inesperado |

O formato do corpo de erro é o mesmo de [Unidades de Medida](unidades-medida.md#formato-de-erro).

## Vínculo com o estoque (planejado)

Quando o módulo de Item for construído, `item` ganhará `planta_id` (UUID, nullable, FK para `planta`) e o nome científico do item passará a ser derivado da planta em vez de duplicado. A coluna e a FK **não existem ainda**; nenhum endpoint deste módulo os antecipa.
