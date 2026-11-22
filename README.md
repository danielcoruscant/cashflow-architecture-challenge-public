# Desafio Arquitetura de Solução

## Objetivo

Desenvolver uma solução para controle de fluxo de caixa diário de um comerciante, permitindo o registro de lançamentos financeiros (créditos e débitos) e a consulta do saldo diário consolidado, por conta e por data.

## Documentação de referência

| Documento | Conteúdo |
|---|---|
| [Plano de implementação](docs/implementation/implementation-plan.md) | Escopo, tarefas, critérios de aceite e dependências |
| [Roadmap de implementação](docs/implementation/implementation-roadmap.md) | Marcos e sequência de execução |
| [Implantação produtiva](docs/operations/production-deployment.md) | Pré-requisitos, Terraform, RBAC, publicação, validação operacional e rollback |
| [ADRs](docs/adr/README.md) | Decisões arquiteturais (ADR-001 a ADR-017) |
| [Context Map](docs/domain/context-map.md) | Domínios, contextos e eventos |
| [Requisitos não funcionais](docs/architecture/non-functional-requirements.md) | NFRs |
| [Segurança](docs/architecture/security-architecture.md), [Riscos](docs/architecture/risk-analysis-and-mitigations.md), [Custos](docs/architecture/azure-cost-estimate.md) | Arquitetura de segurança, riscos e estimativa |
| [Enunciado do desafio](docs/desafio-arquiteto-solucao.md) | Requisitos originais |

## Escopo da entrega

**Escopo definido para implementação:** ledger append-only, CDC via Cosmos Change Feed, Azure Function publisher, Azure Service Bus, Inbox Pattern, consolidação diária, JWT/Entra ID/RBAC por operação, Managed Identity, Application Gateway WAF v2, Application Insights, Terraform (ambiente único `prod`) e GitHub Actions. OpenTelemetry permanece fora do escopo desta entrega.

**Documentado, não implementado:** replay/reprocessamento/reconstrução do Read Model, tratamento de DLQ, multi-região, DR, múltiplos ambientes, Dapr, hardening corporativo avançado, testes de carga em Azure real, provisionamento real e operação produtiva.

**Descartado:** Outbox Pattern (substituído por CDC + Inbox).

> Nenhum recurso Azure é provisionado nesta entrega; Terraform é validado estaticamente. Testes limitados com Cosmos Emulator/Testcontainers foram executados localmente, mas não comprovam autenticação/RBAC nem integração completa com Azure.

## Build e testes locais

Pré-requisitos: JDK 25, Maven, Node.js 20.19+ e npm. Os comandos abaixo não fazem login nem provisionam recursos Azure:

```bash
mvn -B -ntp -f services/lancamentos-service/pom.xml clean verify
mvn -B -ntp -f services/consolidado-service/pom.xml clean verify
mvn -B -ntp -f integrations/cdc-publisher/function/cdc-function/pom.xml test
mvn -B -ntp -f integrations/cdc-consumer/pom.xml clean verify
cd frontend && npm ci && npm run build && npm run test:coverage
```

O empacotamento da Azure Function exige um nome local para o plugin:

```bash
mvn -B -ntp -f integrations/cdc-publisher/function/cdc-function/pom.xml verify \
  -Dazure.functions.appName=ci-validation \
  -Dazure.functions.resourceGroup=ci-validation \
  -Dazure.functions.region=centralus
```

Esse comando apenas gera o pacote local, não publica a Function. Configuração de APIs, variáveis Azure, limitações de integração e execução por componente estão nos READMEs dos serviços e integrações.

Valide também as políticas APIM sem acessar o runtime Azure:

```bash
python3 tests/validate-apim-policies.py
```

---

# Objetivos Arquiteturais

A solução deve demonstrar:

- Escalabilidade
- Resiliência
- Segurança
- Observabilidade
- Baixo acoplamento
- Evolução futura
- Boas práticas de microsserviços

---

# Arquitetura Escolhida

## Estilo Arquitetural

- Domain Driven Design (DDD)
- Microsserviços
- Event Driven Architecture
- CQRS
- API First

---

# Contextos de Negócio

## Contexto de Lançamentos

Responsável por:

- Registrar lançamentos (ledger append-only)
- Compensar lançamentos (sem atualização ou exclusão)
- Consultar lançamentos
- Validar dados financeiros

Entidade principal:

Lançamento

```json
{
  "id": "uuid",
  "accountId": "C001",
  "tipo": "CREDITO|DEBITO",
  "valor": 100.00,
  "data": "2025-06-24"
}
```

Moeda única: BRL. Data de negócio no fuso `America/Sao_Paulo`.

---

## Contexto de Consolidação

Responsável por:

- Receber eventos financeiros
- Garantir idempotência (Inbox Pattern)
- Atualizar saldo consolidado diário por conta e data
- Expor relatório consolidado

Entidade principal:

ConsolidadoDiario

```json
{
  "accountId": "C001",
  "data": "2025-06-24",
  "creditos": 1000,
  "debitos": 300,
  "saldo": 700
}
```

---

# Arquitetura Azure

Acesso: Internet → Application Gateway WAF v2 → Static Web Apps (SPA) e API Management → Container Apps

├── lancamentos-service
└── consolidado-service

Fluxo de eventos:

```text
Lançamentos API
      ↓
Cosmos DB (ledger append-only)
      ↓
Change Feed (CDC)
      ↓
Azure Function (publisher)
      ↓
Azure Service Bus
      ↓
Consolidado (consumidor)
      ↓
Inbox Pattern
      ↓
Projeção do saldo diário (Cosmos DB)
```

Observabilidade desta entrega: Application Insights; OpenTelemetry é evolução futura. Autenticação: Microsoft Entra ID (JWT) e Managed Identity entre recursos.

---

# Requisitos Não Funcionais

Disponibilidade mínima: 99.9% (meta de projeto; não comprovada nesta entrega)

Latência:

- Consultas: P95 < 200 ms
- Escritas: P95 < 500 ms

Escalabilidade:

- mínimo 50 req/s no Consolidado, com perda máxima de 5%

Resiliência:

- falha no consolidado não pode impactar lançamentos

Segurança:

- Autenticação Entra ID
- JWT
- TLS 1.2+

Observabilidade:

- Logs estruturados
- Métricas
- Correlação de logs e eventos por `correlationId`/`causationId` (OpenTelemetry e tracing distribuído permanecem fora do escopo)

---

# Tecnologias

Backend

- Java 25 (LTS)
- Spring Boot 3.x
- Maven
- OpenAPI

Frontend

- React
- TypeScript

Azure

- Azure Container Apps
- Cosmos DB
- Cosmos Change Feed
- Service Bus
- Azure Functions
- Application Insights
- API Management
- Application Gateway WAF v2
- Static Web Apps
- Entra ID

Entrega

- Terraform
- GitHub Actions

---

# Estratégia CQRS

Write Model:

- Serviço de Lançamentos

Read Model:

- Serviço de Consolidado

Comunicação:

- CDC (Change Feed) → Function → Service Bus

---

# Roadmap

O roadmap por sprint está em [implementation-roadmap.md](docs/implementation/implementation-roadmap.md) e o detalhamento por tarefa em [implementation-plan.md](docs/implementation/implementation-plan.md).

O procedimento operacional de produção, incluindo pré-requisitos, publicação, validação e rollback, está no [guia de implantação produtiva](docs/operations/production-deployment.md).

# Estrutura do projeto
```text
cashflow-architecture-challenge

├── frontend
│   ├── src
│   │   ├── app
│   │   ├── pages
│   │   ├── features
│   │   ├── generated
│   │   ├── providers
│   │   ├── routes
│   │   ├── services
│   │   ├── hooks
│   │   ├── contexts
│   │   ├── shared
│   │   └── tests
│   ├── public
│   ├── .env.example
│   ├── package.json
│   └── README.md
├── services
│   ├── lancamentos-service
│   │   ├── pom.xml
│   │   ├── src/main/java/br/com/cashflow/lancamentos
│   │   │   ├── application
│   │   │   │   ├── command
│   │   │   │   ├── query
│   │   │   │   ├── usecase
│   │   │   │   └── dto
│   │   │   ├── domain
│   │   │   │   ├── model
│   │   │   │   ├── repository
│   │   │   │   ├── service
│   │   │   │   ├── event
│   │   │   │   └── exception
│   │   │   ├── infrastructure
│   │   │   │   ├── cosmos
│   │   │   │   ├── persistence
│   │   │   │   └── mapper
│   │   │   ├── interfaces
│   │   │   │   ├── api
│   │   │   │   ├── request
│   │   │   │   └── response
│   │   │   └── configuration
│   │   ├── src/main/resources
│   │   └── src/test
│   │       ├── java/br/com/cashflow/lancamentos
│   │       │   ├── unit
│   │       │   ├── integration
│   │       │   └── contract
│   │       └── resources
│   └── consolidado-service
│       ├── pom.xml
│       ├── src/main/java/br/com/cashflow/consolidado
│       │   ├── application
│       │   ├── domain
│       │   ├── infrastructure
│       │   ├── interfaces
│       │   └── configuration
│       ├── src/main/resources
│       └── src/test
│           ├── java/br/com/cashflow/consolidado
│           │   ├── unit
│           │   ├── integration
│           │   └── contract
│           └── resources

├── contracts
│   ├── lancamentos-api
│   │   └── openapi.yaml
│   └── consolidado-api
│       └── openapi.yaml

├── integrations
│   ├── cdc-publisher
│   │   └── function
│   │       └── cdc-function        # Azure Function (Change Feed → Service Bus)
│   └── cdc-consumer                # consumidor demonstrativo (isolado; não usar em consolidado-sub)

├── docs
│   ├── adr                         # ADR-001 a ADR-017 + índice
│   ├── architecture                # NFRs, segurança, riscos, custos
│   ├── diagram                     # arquivos .drawio
│   ├── domain                      # context map
│   └── implementation              # plano de implementação e roadmap

├── infra
│   ├── modules
│   └── environments
│       └── prod                    # ambiente único (Dev/HML/QA/Sandbox: documentados)

├── .github
│   ├── workflows                   # CI e deploy (GitHub Actions)
│   ├── instructions
│   └── prompts
```
