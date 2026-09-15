# Farmácia Viva — CREMIC

Sistema de controle de estoque da Farmácia Viva do CREMIC. Esta primeira versão é independente do SGH e cobrirá itens, categorias, unidades de medida, linhas terapêuticas, localizações, lotes, movimentações, saldo, validade, usuários e auditoria.

Este repositório contém a fundação técnica e um protótipo operacional do estoque para demonstração ao PET-Saúde e ao CREMIC. O frontend permite navegar por painel, saldos, lotes, validade, alertas e movimentações usando dados demonstrativos persistidos localmente. O backend dos módulos de estoque será integrado posteriormente.

## Stack

Backend: Java 21, Spring Boot 3, Maven, Spring Web, Spring Data JPA, Hibernate, Spring Validation, Spring Security, JWT (jjwt), PostgreSQL 16, Flyway, MapStruct, Lombok, JUnit 5, Mockito e Testcontainers.

Frontend: React, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod, Tailwind CSS e shadcn/ui (configurado via `components.json`; os componentes são adicionados sob demanda com `npx shadcn@latest add <componente>`).

Infraestrutura: Docker Compose para o PostgreSQL local e GitHub para versionamento.

## Estrutura do repositório

```
farmacia-viva/
├── README.md
├── .gitignore
├── docker-compose.yml
├── backend/
│   ├── pom.xml
│   ├── .gitignore
│   └── src/
│       ├── main/
│       │   ├── java/br/org/cremic/farmaciaviva/
│       │   │   ├── FarmaciaVivaApplication.java
│       │   │   ├── config/
│       │   │   │   ├── ClockConfig.java
│       │   │   │   ├── CorsProperties.java
│       │   │   │   └── SecurityConfig.java
│       │   │   ├── health/
│       │   │   │   ├── HealthController.java
│       │   │   │   ├── HealthResponse.java
│       │   │   │   └── HealthService.java
│       │   │   └── shared/exception/
│       │   │       ├── ApiError.java
│       │   │       ├── BusinessRuleException.java
│       │   │       ├── GlobalExceptionHandler.java
│       │   │       └── ResourceNotFoundException.java
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       ├── application-test.yml
│       │       └── db/migration/V1__baseline.sql
│       └── test/java/br/org/cremic/farmaciaviva/
│           ├── health/
│           │   ├── HealthControllerTest.java
│           │   └── HealthServiceTest.java
│           └── integration/
│               ├── AbstractIntegrationTest.java
│               └── HealthControllerIT.java
└── frontend/
    ├── package.json
    ├── vite.config.ts
    ├── tsconfig.json
    ├── tsconfig.app.json
    ├── tsconfig.node.json
    ├── tailwind.config.ts
    ├── postcss.config.js
    ├── components.json
    ├── index.html
    ├── .env.example
    ├── .env.development
    ├── .gitignore
    └── src/
        ├── main.tsx
        ├── App.tsx
        ├── index.css
        ├── env.ts
        ├── vite-env.d.ts
        ├── lib/
        │   ├── api.ts
        │   ├── health.ts
        │   └── utils.ts
        └── pages/
            └── HealthPage.tsx
```

## Arquitetura do backend

O backend é organizado por módulo de negócio. Cada módulo (`health`, `unidademedida` e `planta` hoje; futuramente `item`, `lote`, `movimentacao`) reúne seu controller, service, repository, DTOs e mappers no mesmo pacote. O pacote `shared` guarda o que é transversal (exceções, erro padrão da API, normalização de texto em `NormalizadorTexto`) e o pacote `config` guarda a configuração da aplicação (segurança, CORS, clock).

Regras que valem para todo o projeto:

Controllers recebem e devolvem apenas DTOs; entidades JPA nunca são expostas na API. As regras de negócio ficam nos services e os repositories cuidam exclusivamente de persistência e consultas. O schema do banco é gerenciado unicamente pelo Flyway, com `spring.jpa.hibernate.ddl-auto=validate` para que o Hibernate apenas valide o mapeamento contra o schema. Chaves primárias são UUID (a migration `V1__baseline.sql` habilita a extensão `pgcrypto`, que fornece `gen_random_uuid()`). Datas de eventos usam `OffsetDateTime` e são preenchidas pela auditoria do Spring Data JPA a partir do bean `Clock` da aplicação (`JpaAuditingConfig`), o que mantém um único ponto de verdade para o tempo; fabricação, entrada e validade usam `LocalDate`. Quantidades de estoque usam sempre `BigDecimal`, nunca `float` ou `double`. Toda alteração crítica é transacional. O frontend nunca acessa o PostgreSQL diretamente; toda comunicação passa pela API REST.

Regras centrais do estoque que orientarão os próximos módulos: o estoque é controlado por item, lote e localização; o saldo é sempre a soma dos lançamentos e não pode ser editado diretamente; movimentações confirmadas não podem ser apagadas e correções são feitas por estorno; saldo negativo não é permitido; transferências geram um lançamento negativo na origem e um positivo no destino; itens e lotes usam arquivamento lógico; lotes vencidos, reprovados ou bloqueados não podem ser consumidos; lotes sujeitos a controle de qualidade respeitam a situação do LCQ; operações de saída tratam concorrência.

## Repositório de Plantas

Primeiro módulo do projeto com backend real e frontend consumindo a API (rota `/repositorio`). Pacote `br.org.cremic.farmaciaviva.planta`; migration `V3__create_planta.sql`; documentação em [docs/api/plantas.md](docs/api/plantas.md).

Quatro tabelas: `planta` (ficha; o nome científico é a identidade, único na forma normalizada por `NormalizadorTexto`), `planta_nome_popular` (único por planta e nome normalizado; plantas diferentes podem compartilhar nome popular — é essa ambiguidade que o repositório torna visível), `planta_similar` (autorrelacionamento armazenado direcionado, uma linha por sentido; o service cria o recíproco ao vincular e remove os dois ao desvincular, e arquivar uma planta preserva os vínculos) e `planta_referencia` (título ou texto livre obrigatório, no banco e no service).

A listagem busca em nome científico e nomes populares pelas colunas normalizadas e evita N+1 hidratando os nomes populares da página em um único fetch join por `id IN (...)` — fetch join direto na consulta paginada faria o Hibernate paginar em memória.

Seed de desenvolvimento (`PlantaSeedDev`, só no profile `dev`): quatro plantas reais (Mikania glomerata, Mikania laevigata, Maytenus ilicifolia, Lippia alba) com nomes populares, referências de tipos variados e um par vinculado como similar. Idempotente: procura pelo nome científico normalizado, nunca por UUID fixo; rodar duas vezes não duplica.

Vínculo com o estoque (planejado, não criado): quando o módulo de Item for construído, `item` ganha `planta_id` (UUID, nullable, FK para `planta`) e o nome científico do item passa a ser derivado da planta em vez de duplicado. A tabela `item` ainda não existe, então a migration V3 não cria a coluna nem a FK, e não há código no backend antecipando esse vínculo.

## Segurança

O Spring Security está configurado como API stateless (sem sessão e sem CSRF). Nesta fase, `GET /api/v1/health` é público e as rotas `/api/v1/unidades-medida/**` e `/api/v1/plantas/**` estão temporariamente liberadas no `SecurityConfig`; qualquer outra rota exige autenticação e responde 401/403. A autenticação por JWT (login, emissão e validação de token, perfis de usuário) será implementada na tarefa do módulo de usuários; as dependências jjwt já estão no `pom.xml`. O CORS libera a origem do frontend de desenvolvimento (`http://localhost:5173`) e é configurável pela propriedade `app.cors.allowed-origins`.

## Tratamento de erros

O `GlobalExceptionHandler` (`@RestControllerAdvice`) converte exceções em um corpo JSON padronizado (`ApiError`) com `timestamp`, `status`, `error`, `message`, `path` e, quando houver, `fieldErrors`. Mapeamentos: `ResourceNotFoundException` responde 404; `BusinessRuleException` responde 422; `DuplicateResourceException` e `DataIntegrityViolationException` respondem 409; `OptimisticLockingFailureException` responde 409; erros de validação (`MethodArgumentNotValidException`, `ConstraintViolationException`) respondem 400 com a lista de campos inválidos; corpo malformado, parâmetro ausente, parâmetro com tipo inválido e campo de ordenação inexistente (`PropertyReferenceException`) respondem 400; método não suportado responde 405; `Content-Type` não suportado responde 415; rota inexistente responde 404; qualquer erro inesperado responde 500 com mensagem genérica e log completo no servidor.

## Profiles

`dev`: usa o PostgreSQL do Docker Compose em `localhost:5432` (banco `farmacia_viva`, usuário `farmacia`, senha `farmacia`) e liga logs de SQL. As credenciais podem ser sobrescritas por variáveis de ambiente do Spring (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).

`test`: usado pelos testes de integração. O datasource é fornecido automaticamente pelo Testcontainers através de `@ServiceConnection`, que sobe um contêiner `postgres:16-alpine` descartável, executa as migrations do Flyway e valida o mapeamento JPA.

## Testes

`HealthServiceTest` e `HealthControllerTest` são testes unitários (JUnit 5 e Mockito, sem contexto Spring e sem banco). `HealthControllerIT` estende `AbstractIntegrationTest`, sobe a aplicação completa com PostgreSQL real via Testcontainers e verifica o endpoint de health, a execução das migrations do Flyway e a proteção das rotas não liberadas. Os testes de integração exigem Docker em execução na máquina.

### Rodando os testes

| Comando | O que roda | Exige Docker |
|---|---|---|
| `mvn test` | Somente os testes unitários (`*Test`), via surefire | Não |
| `mvn verify` | Os unitários **e** os testes de integração (`*IT`), via failsafe | Sim |

Os testes de integração sobem um PostgreSQL 16 em container via Testcontainers e falham o build (`failsafe:verify`) se qualquer um quebrar. A separação entre `*Test` e `*IT` é declarada explicitamente no `pom.xml` (exclusão no surefire, inclusão no failsafe), não depende de convenção de nomes.

Sobre a propriedade `api.version`: ela já vai fixada no `pom.xml` (`docker.api.version`) e é repassada ao JVM dos testes pelo failsafe — não é preciso passar nada na linha de comando. Ela existe porque o `docker-java-core` embutido (shaded) no Testcontainers negocia por padrão a API Docker 1.32, e o Docker Engine 29 só aceita ≥ 1.40. Sem ela, os ITs falham com `client version 1.32 is too old`. Variáveis de ambiente como `DOCKER_API_VERSION` e o arquivo `~/.testcontainers.properties` **não** substituem essa propriedade.

## Frontend

O frontend utiliza React Router, React Hook Form, Zod, Tailwind e Lucide. A página inicial é o painel operacional; a rota `/status` apresenta o estado da persistência local e as limitações explícitas do protótipo.

A rota `/repositorio` (Repositório de Plantas) consome a API REST de verdade, com TanStack Query e schemas Zod que espelham os DTOs do backend campo a campo. As demais telas continuam em dados demonstrativos locais.

O protótipo de estoque usa a chave versionada `cremic-estoque-demo-v1` no `localStorage`. Os dados fictícios são criados somente no primeiro acesso, validados com Zod e acessados por uma interface de repositório substituível futuramente por API REST. A opção “Restaurar dados de demonstração” exige confirmação e nunca apaga dados automaticamente.

Variáveis de ambiente: copie `.env.example` para `.env` se quiser sobrescrever localmente; `.env.development` já aponta para `http://localhost:8080`. Arquivos `.env` e `.env.production` são ignorados pelo Git.

## Pré-requisitos

Java 21 (JDK), Maven 3.9 ou superior, Docker e Docker Compose, Node.js 20 ou superior e npm.

## Como executar

### Opção rápida: um único comando

Depois de instalar os pré-requisitos (Docker, SDKMAN/Java 21/Maven e nvm/Node), suba tudo de uma vez com:

```bash
./start.sh
```

O script sobe o PostgreSQL via Docker, aguarda ele ficar pronto, sobe o backend (profile `dev`), aguarda o `/api/v1/health` responder, sobe o frontend (`npm install` na primeira vez) e mostra os logs de backend e frontend juntos no mesmo terminal. `Ctrl+C` derruba backend e frontend (o Postgres continua rodando no Docker; para parar também, use `docker compose down`).

Logs completos ficam em `.run/backend.log` e `.run/frontend.log`.

### Passo a passo manual

1. Subir o PostgreSQL:

```bash
docker compose up -d postgres
```

2. Executar o backend (profile `dev`):

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

A API ficará disponível em `http://localhost:8080` e o health em `http://localhost:8080/api/v1/health`.

3. Executar os testes do backend (o Docker precisa estar em execução para os testes de integração):

```bash
cd backend
mvn test
```

4. Instalar as dependências e iniciar o frontend:

```bash
cd frontend
npm install
npm run dev
```

O frontend ficará disponível em `http://localhost:5173`.

## Verificação rápida

```bash
curl http://localhost:8080/api/v1/health
```

Resposta esperada:

```json
{
  "status": "UP",
  "application": "farmacia-viva-backend",
  "timestamp": "2026-08-04T12:00:00Z"
}
```

## Deploy

A produção roda no Railway (projeto `farmacia-viva`, ambiente `production`). Os serviços são construídos pelo builder Railpack e fazem deploy automático a cada push na branch `main`. Build command, start command e root directory ficam nas configurações de cada serviço no painel do Railway, não em arquivos do repositório.

### Arquitetura no Railway

Três serviços no mesmo projeto:

| Serviço | Root directory | URL |
|---|---|---|
| `Postgres` | — (banco gerenciado, com volume) | sem domínio público; o backend acessa pela rede privada (`postgres.railway.internal`) |
| `backend` | `backend` | https://backend-production-2980.up.railway.app |
| `front` | `frontend` | https://farmacia-viva.up.railway.app |

O serviço do frontend se chama `front`, não `frontend`. É esse o nome que o CLI aceita: `railway variables --service front`.

### Variáveis do backend

| Variável | Valor | Por quê |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `railway` | Ativa o `application-railway.yml`, que lê porta, banco e CORS do ambiente. |
| `PGHOST` | `${{Postgres.PGHOST}}` | Referência ao serviço Postgres (ver abaixo). |
| `PGPORT` | `${{Postgres.PGPORT}}` | Idem. |
| `PGDATABASE` | `${{Postgres.PGDATABASE}}` | Idem. |
| `PGUSER` | `${{Postgres.PGUSER}}` | Idem. |
| `PGPASSWORD` | `${{Postgres.PGPASSWORD}}` | Idem. |
| `CORS_ORIGINS` | `https://farmacia-viva.up.railway.app` | Origem que o navegador pode usar para chamar a API. |

**Por que não usar a `DATABASE_URL` pronta.** O Railway entrega a `DATABASE_URL` no formato `postgresql://usuario:senha@host:porta/banco`, mas o driver JDBC do Spring exige `jdbc:postgresql://host:porta/banco`, com usuário e senha separados. Por isso o `application-railway.yml` monta a URL a partir das variáveis separadas:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
    username: ${PGUSER}
    password: ${PGPASSWORD}
```

As variáveis são referências (`${{Postgres.PGHOST}}` etc.), não valores copiados: as credenciais continuam tendo um único dono, o serviço Postgres. Se elas mudarem lá, basta redeployar o backend.

**`CORS_ORIGINS`**: a URL do frontend com `https` e sem barra no final, exatamente como o navegador envia no header `Origin`. Uma origem com `http://` no lugar de `https://` é recusada. Sem a variável, o profile cai no padrão `http://localhost:5173` e toda chamada do frontend publicado recebe `403 Invalid CORS request`.

`PORT` não precisa ser setada: o Railway injeta a variável e o `application-railway.yml` usa `server.port: ${PORT:8080}`.

### Variável do frontend

| Variável | Valor |
|---|---|
| `VITE_API_BASE_URL` | `https://backend-production-2980.up.railway.app` |

Duas pegadinhas que já aconteceram:

1. **O nome é `VITE_API_BASE_URL`**, lido em `frontend/src/env.ts`. Já setamos `VITE_API_URL` por engano e isso custou uma sessão de debug. Uma variável com nome errado não gera erro nenhum: o Vite ignora a variável, e o build embute o que estiver em `VITE_API_BASE_URL`. Naquele caso, era `http://localhost:8080`.
2. **O valor é a raiz do backend, sem `/api/v1`.** Cada chamada já traz o prefixo (`/api/v1/plantas` em `services/plantasApi.ts`, `/api/v1/health` em `lib/health.ts`), e o `lib/api.ts` só concatena base + path. Com `/api/v1` no valor, as chamadas viram `/api/v1/api/v1/plantas`. Essa rota não existe e não está entre as liberadas no `SecurityConfig`, então o Spring Security responde **403 com corpo vazio**, e não 404, antes de a requisição chegar ao controller. A tela mostra "HTTP 403".

### Variáveis `VITE_*` são embutidas no BUILD

O Vite substitui `import.meta.env.VITE_*` pelo valor literal no momento do `vite build`. O bundle publicado é um arquivo estático e não lê variável nenhuma em tempo de execução. Portanto:

- **Mudar a variável não afeta o que já está publicado.** É preciso que o frontend seja construído de novo (redeploy).
- **Confira se o redeploy aconteceu mesmo** com `railway deployment list --service front`. Em 15/09/2026, setar uma variável pelo CLI disparou deploy, mas removê-la (`railway variable delete`) não disparou; foi preciso rodar `railway redeploy --service front`.
- **O hash do arquivo em `/assets` só muda se o conteúdo mudar.** O nome (`index-<hash>.js`) é derivado do conteúdo gerado. Um commit vazio com a variável inalterada gera exatamente o mesmo bundle, com o mesmo hash. Hash igual não indica cache, indica que nada mudou de fato no que foi compilado. Pelo mesmo motivo, remover uma variável que nenhum código lê também mantém o hash.

### Build e start

| Serviço | Build command | Start command |
|---|---|---|
| `backend` | `./mvnw clean package -DskipTests` | `java -jar target/*.jar` |
| `front` | `npm install --no-audit --no-fund && npm run build` | `npx serve -s dist -l $PORT` |

O `-s` do `serve` devolve o `index.html` para qualquer rota desconhecida, o que permite abrir diretamente rotas do React Router como `/repositorio`.

**Por que `npm install` e não `npm ci`.** Ao detectar um projeto Vite (dependência `vite`, script `build` e `vite.config.ts`), o Railpack monta `/app/node_modules/.vite` como volume de cache no passo de build. O `npm ci` começa apagando o `node_modules` inteiro e não consegue remover o ponto de montagem:

```
npm error code EBUSY
npm error syscall rmdir
npm error path /app/node_modules/.vite
npm error EBUSY: resource busy or locked, rmdir '/app/node_modules/.vite'
```

O `npm install` não apaga o diretório inteiro, então não colide com o volume.

O `vite.config.ts` define `cacheDir: ".vite-cache"`, mas isso **não** libera o uso de `npm ci`. O volume é criado pelo Railpack a partir da detecção do projeto, independentemente da configuração do Vite (`isVitePackage` e `addCachesToBuildStep` em `core/providers/node` do [railpack](https://github.com/railwayapp/railpack)). Além disso, o `vite build` não usa o `cacheDir`; quem usa é o servidor de desenvolvimento (`npm run dev`).

### Diagnóstico rápido

Comandos que fecharam o último problema de deploy e servem para qualquer regressão:

```bash
FRONT=https://farmacia-viva.up.railway.app
BACK=https://backend-production-2980.up.railway.app

# 1. Endpoint com a origem do frontend: olhe o status, o header access-control-allow-origin e o corpo
curl -i -H "Origin: $FRONT" $BACK/api/v1/plantas

# 2. Hash do bundle publicado (o hash pode conter - e _)
BUNDLE=$(curl -s $FRONT/ | grep -o 'index-[A-Za-z0-9_-]*\.js')
echo $BUNDLE

# 3. O que ficou embutido no bundle
curl -s $FRONT/assets/$BUNDLE | grep -o 'https://[a-z0-9.-]*railway.app[^"]*' | sort -u
curl -s $FRONT/assets/$BUNDLE | grep -o 'localhost:[0-9]*' | sort -u

# 4. Variáveis e deploys do frontend
railway variables --service front --kv
railway deployment list --service front
```

Cuidado: `railway variables --service backend --kv` imprime a senha do banco em texto puro.

| O que aparece | Significado | Onde corrigir |
|---|---|---|
| `200` com `access-control-allow-origin` igual à URL do front | Backend e CORS corretos; se a tela ainda falha, olhe o bundle | — |
| `403` com corpo `Invalid CORS request` e sem `access-control-allow-origin` | Origem recusada pelo CORS | `CORS_ORIGINS` no backend (ausente, `http` em vez de `https`, domínio errado) |
| `403` com corpo vazio | Rota não liberada no `SecurityConfig` **ou rota inexistente**: o Security barra antes de chegar ao 404 | `SecurityConfig`, ou o path da chamada |
| `404` com JSON (`ApiError`) | Rota liberada, mas o endpoint ou o recurso não existe | Path da chamada / módulo no backend |
| `localhost` no bundle | `VITE_API_BASE_URL` errada no build, ou bundle antigo | Variável do `front` + redeploy |
| URL `railway.app` terminando em `/api/v1` no bundle | Prefixo duplicado no valor | Tirar `/api/v1` de `VITE_API_BASE_URL` + redeploy |

Na tela, "Não foi possível conectar ao servidor" vem do `lib/api.ts` quando o `fetch` falha sem resposta: URL errada no bundle (por exemplo `localhost`), backend fora do ar ou CORS bloqueado pelo navegador. Quando há resposta HTTP com erro, a tela mostra o status, como em "HTTP 403".

## Documentação da API

A documentação detalhada dos endpoints fica em `docs/api/`. Módulos documentados: [Unidades de Medida](docs/api/unidades-medida.md) (`/api/v1/unidades-medida`), com criação, listagem paginada com filtro por situação, busca por id, atualização, arquivamento lógico e reativação; [Repositório de Plantas](docs/api/plantas.md) (`/api/v1/plantas`), com ficha completa, busca por nome científico ou popular, similares recíprocos, referências e arquivamento lógico. Enquanto a autenticação JWT não é implementada, os endpoints de unidades de medida estão temporariamente liberados no `SecurityConfig`; o health continua público e todas as demais rotas exigem autenticação.

## Próximas tarefas

Módulo de usuários com autenticação JWT e perfis de acesso; demais cadastros básicos (categorias, linhas terapêuticas, localizações); itens com arquivamento lógico; lotes com validade, situação de qualidade (LCQ) e bloqueios; movimentações com estorno, transferência entre localizações, cálculo de saldo por soma de lançamentos, bloqueio de saldo negativo e controle de concorrência nas saídas; trilha de auditoria das operações.
