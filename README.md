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

## Documentação da API

A documentação detalhada dos endpoints fica em `docs/api/`. Módulos documentados: [Unidades de Medida](docs/api/unidades-medida.md) (`/api/v1/unidades-medida`), com criação, listagem paginada com filtro por situação, busca por id, atualização, arquivamento lógico e reativação; [Repositório de Plantas](docs/api/plantas.md) (`/api/v1/plantas`), com ficha completa, busca por nome científico ou popular, similares recíprocos, referências e arquivamento lógico. Enquanto a autenticação JWT não é implementada, os endpoints de unidades de medida estão temporariamente liberados no `SecurityConfig`; o health continua público e todas as demais rotas exigem autenticação.

## Próximas tarefas

Módulo de usuários com autenticação JWT e perfis de acesso; demais cadastros básicos (categorias, linhas terapêuticas, localizações); itens com arquivamento lógico; lotes com validade, situação de qualidade (LCQ) e bloqueios; movimentações com estorno, transferência entre localizações, cálculo de saldo por soma de lançamentos, bloqueio de saldo negativo e controle de concorrência nas saídas; trilha de auditoria das operações.
