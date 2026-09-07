# Farmácia Viva — CREMIC

Sistema de controle de estoque da Farmácia Viva do CREMIC. Esta primeira versão é independente do SGH e cobrirá itens, categorias, unidades de medida, linhas terapêuticas, localizações, lotes, movimentações, saldo, validade, usuários e auditoria.

Este repositório contém a fundação do projeto: backend Spring Boot, frontend React, banco PostgreSQL via Docker Compose, migrations com Flyway, profiles `dev` e `test`, tratamento global de erros, testes unitários e de integração com Testcontainers e o endpoint `GET /api/v1/health`. Os módulos de negócio (Item, Lote, Movimentação e demais) serão implementados nas próximas tarefas.

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

O backend é organizado por módulo de negócio. Cada módulo (por exemplo `health` hoje e, futuramente, `item`, `lote`, `movimentacao`) reúne seu controller, service, repository, DTOs e mappers no mesmo pacote. O pacote `shared` guarda o que é transversal (exceções, erro padrão da API) e o pacote `config` guarda a configuração da aplicação (segurança, CORS, clock).

Regras que valem para todo o projeto:

Controllers recebem e devolvem apenas DTOs; entidades JPA nunca são expostas na API. As regras de negócio ficam nos services e os repositories cuidam exclusivamente de persistência e consultas. O schema do banco é gerenciado unicamente pelo Flyway, com `spring.jpa.hibernate.ddl-auto=validate` para que o Hibernate apenas valide o mapeamento contra o schema. Chaves primárias são UUID (a migration `V1__baseline.sql` habilita a extensão `pgcrypto`, que fornece `gen_random_uuid()`). Datas de eventos usam `OffsetDateTime` e são preenchidas pela auditoria do Spring Data JPA a partir do bean `Clock` da aplicação (`JpaAuditingConfig`), o que mantém um único ponto de verdade para o tempo; fabricação, entrada e validade usam `LocalDate`. Quantidades de estoque usam sempre `BigDecimal`, nunca `float` ou `double`. Toda alteração crítica é transacional. O frontend nunca acessa o PostgreSQL diretamente; toda comunicação passa pela API REST.

Regras centrais do estoque que orientarão os próximos módulos: o estoque é controlado por item, lote e localização; o saldo é sempre a soma dos lançamentos e não pode ser editado diretamente; movimentações confirmadas não podem ser apagadas e correções são feitas por estorno; saldo negativo não é permitido; transferências geram um lançamento negativo na origem e um positivo no destino; itens e lotes usam arquivamento lógico; lotes vencidos, reprovados ou bloqueados não podem ser consumidos; lotes sujeitos a controle de qualidade respeitam a situação do LCQ; operações de saída tratam concorrência.

## Segurança

O Spring Security está configurado como API stateless (sem sessão e sem CSRF). Nesta fase, apenas `GET /api/v1/health` é público; qualquer outra rota exige autenticação e responde 401/403. A autenticação por JWT (login, emissão e validação de token, perfis de usuário) será implementada na tarefa do módulo de usuários; as dependências jjwt já estão no `pom.xml`. O CORS libera a origem do frontend de desenvolvimento (`http://localhost:5173`) e é configurável pela propriedade `app.cors.allowed-origins`.

## Tratamento de erros

O `GlobalExceptionHandler` (`@RestControllerAdvice`) converte exceções em um corpo JSON padronizado (`ApiError`) com `timestamp`, `status`, `error`, `message`, `path` e, quando houver, `fieldErrors`. Mapeamentos: `ResourceNotFoundException` responde 404; `BusinessRuleException` responde 422; `DuplicateResourceException` e `DataIntegrityViolationException` respondem 409; `OptimisticLockingFailureException` responde 409; erros de validação (`MethodArgumentNotValidException`, `ConstraintViolationException`) respondem 400 com a lista de campos inválidos; corpo malformado, parâmetro ausente, parâmetro com tipo inválido e campo de ordenação inexistente (`PropertyReferenceException`) respondem 400; método não suportado responde 405; `Content-Type` não suportado responde 415; rota inexistente responde 404; qualquer erro inesperado responde 500 com mensagem genérica e log completo no servidor.

## Profiles

`dev`: usa o PostgreSQL do Docker Compose em `localhost:5432` (banco `farmacia_viva`, usuário `farmacia`, senha `farmacia`) e liga logs de SQL. As credenciais podem ser sobrescritas por variáveis de ambiente do Spring (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).

`test`: usado pelos testes de integração. O datasource é fornecido automaticamente pelo Testcontainers através de `@ServiceConnection`, que sobe um contêiner `postgres:16-alpine` descartável, executa as migrations do Flyway e valida o mapeamento JPA.

## Testes

`HealthServiceTest` e `HealthControllerTest` são testes unitários (JUnit 5 e Mockito, sem contexto Spring e sem banco). `HealthControllerIT` estende `AbstractIntegrationTest`, sobe a aplicação completa com PostgreSQL real via Testcontainers e verifica o endpoint de health, a execução das migrations do Flyway e a proteção das rotas não liberadas. Os testes de integração exigem Docker em execução na máquina.

## Frontend

O frontend valida as variáveis de ambiente com Zod em `src/env.ts` e falha na inicialização se `VITE_API_BASE_URL` estiver ausente ou inválida. O cliente HTTP em `src/lib/api.ts` centraliza as chamadas à API; `src/lib/health.ts` valida a resposta do backend com um schema Zod. A página inicial consome `GET /api/v1/health` com TanStack Query, com revalidação automática a cada 30 segundos. React Hook Form e shadcn/ui já estão instalados e configurados para os formulários dos próximos módulos.

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

A documentação detalhada dos endpoints fica em `docs/api/`. Módulos documentados: [Unidades de Medida](docs/api/unidades-medida.md) (`/api/v1/unidades-medida`), com criação, listagem paginada com filtro por situação, busca por id, atualização, arquivamento lógico e reativação. Enquanto a autenticação JWT não é implementada, os endpoints de unidades de medida estão temporariamente liberados no `SecurityConfig`; o health continua público e todas as demais rotas exigem autenticação.

## Próximas tarefas

Módulo de usuários com autenticação JWT e perfis de acesso; demais cadastros básicos (categorias, linhas terapêuticas, localizações); itens com arquivamento lógico; lotes com validade, situação de qualidade (LCQ) e bloqueios; movimentações com estorno, transferência entre localizações, cálculo de saldo por soma de lançamentos, bloqueio de saldo negativo e controle de concorrência nas saídas; trilha de auditoria das operações.
