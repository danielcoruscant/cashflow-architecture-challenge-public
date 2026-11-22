# Requisitos Não Funcionais

## Objetivo

Este documento define os requisitos não funcionais da solução de Controle de Fluxo de Caixa, alinhados aos objetivos de negócio, requisitos obrigatórios e critérios de qualidade da arquitetura. 【1-010f72】

---

# 1. Disponibilidade

## NFR-001

O sistema deve possuir disponibilidade mínima de:

99,9%

para operações de registro de lançamentos.

> Meta de projeto. Não é comprovada nesta entrega (sem provisionamento nem medição em Azure) e depende dos SKUs escolhidos; por exemplo, o APIM Developer não oferece SLA (ADR-014).

### Estratégias

- Azure Container Apps
- Cosmos DB gerenciado
- Azure Service Bus
- Serviços gerenciados Azure

---

# 2. Escalabilidade

## NFR-002

O sistema deve suportar crescimento horizontal sem interrupção.

### Estratégias

- Escalabilidade automática do Azure Container Apps
- Cosmos DB com escalabilidade automática
- Azure Service Bus desacoplando produtores e consumidores

---

# 3. Desempenho

## NFR-003

As APIs devem responder dentro dos seguintes limites:

### Operações de escrita

P95 < 500ms

### Operações de consulta

P95 < 200ms

---

## NFR-004

O serviço de Consolidado deve suportar:

50 requisições por segundo

com perda máxima de:

5%

conforme requisito do desafio. 【1-010f72】

---

# 4. Resiliência

## NFR-005

Falhas do serviço de Consolidado não devem impactar o serviço de Lançamentos. 

### Estratégias

- Event Driven Architecture
- Azure Service Bus
- Cosmos Change Feed
- CDC Publisher

---

## NFR-006

Mensagens devem ser processadas de forma idempotente e redeliveradas em falhas transitórias.

### Estratégias

- Inbox Pattern (ADR-012)
- MessageId = eventId
- Retry por lock e redelivery

> Fora do escopo desta entrega: tratamento de Dead Letter Queue (ADR-015) e reprocessamento/replay (ADR-009).

---

# 5. Consistência

## NFR-007

O sistema adotará Consistência Eventual para projeções do Consolidado.

### Estratégias

- CQRS
- Read Model
- Cosmos Change Feed

---

## NFR-008

Não deve haver aplicação duplicada de eventos ao saldo consolidado.

### Estratégias

- Inbox Pattern (mecanismo principal)
- TransactionalBatch (Inbox + projeção na mesma partição)
- ETag (concorrência otimista)

A consistência de negócio não depende da Duplicate Detection do Service Bus.

---

# 6. Auditoria

## NFR-009

Todo lançamento deve possuir histórico permanente.

### Estratégias

- Immutable Ledger
- Append Only

Não serão permitidos:

- Updates
- Deletes

Correções ocorrerão por lançamentos compensatórios.

---

# 7. Segurança

## NFR-010

Toda comunicação deve utilizar:

TLS 1.2+

ou superior.

---

## NFR-011

Usuários devem ser autenticados através do Microsoft Entra ID.

### Estratégias

- OpenID Connect
- OAuth2 Authorization Code Flow
- PKCE

---

## NFR-012

Integrações internas devem utilizar autenticação sem credenciais estáticas.

### Estratégias

- Managed Identity
- Azure RBAC (Cosmos DB por database e Service Bus: sender na Function, receiver no Consolidado)

---

## NFR-013

As APIs devem aplicar:

- JWT Validation
- Rate Limit de 120 chamadas por minuto por principal autenticado (baseline configurável)
- Governance Policies

através do Azure API Management. Os serviços validam o JWT novamente e aplicam RBAC por operação (ADR-017).

As políticas APIM aplicam o limite por claim `oid`, com fallback para `sub`.
O limite não foi calibrado por teste de carga e deve ser revisto antes de uso
produtivo.

---

# 8. Observabilidade

## NFR-014

Todos os componentes devem gerar telemetria centralizada.

### Componentes

- Frontend
- Lançamentos Service
- Consolidado Service
- CDC Publisher

---

## NFR-015

Todas as exceções devem ser registradas.

### Estratégias

- OpenTelemetry
- Application Insights
- Log Analytics Workspace
- Logs estruturados com `correlationId`

---

## NFR-016

A solução deve permitir correlação ponta a ponta do fluxo de uma operação por `correlationId` e `causationId`. Esses identificadores não substituem spans de tracing distribuído; OpenTelemetry permanece fora do escopo desta entrega.

Fluxo:

Lançamento
→ Cosmos DB (ledger)
→ Change Feed
→ CDC Publisher
→ Service Bus
→ Consolidado (Inbox + projeção)

---

# 9. Operação

## NFR-017

A solução deve ser totalmente implantável por Infraestrutura como Código.

### Estratégias

- Terraform (ambiente único `prod`, ADR-014). Nesta entrega o Terraform é validado estaticamente; não há provisionamento real.

---

## NFR-018

A solução deve suportar deploy independente dos serviços.

### Componentes

- Frontend
- Lançamentos Service
- Consolidado Service
- CDC Publisher

---

# 10. Manutenibilidade

## NFR-019

Os serviços devem possuir baixo acoplamento.

### Estratégias

- Event Driven Architecture
- CQRS
- Service Bus

---

## NFR-020

Mudanças no serviço de Consolidado não devem exigir alteração do serviço de Lançamentos.

---

# 11. Recuperação de Falhas

## NFR-021

Eventos não processados devem poder ser reprocessados.

**Situação nesta entrega: documentado, não implementado** (ADR-009, ADR-015). O ledger preserva a fonte para reprocessamento futuro; não há replay nem tratamento de DLQ.

### Estratégias (evolução)

- Replay Service
- Cosmos Change Feed
- Inbox Pattern (deduplicação)

---

## NFR-022

O Consolidado deve ser reconstruível a partir da origem dos dados.

**Situação nesta entrega: documentado, não implementado** (ADR-009). A reconstrução automatizada é evolução futura.

### Estratégias (evolução)

- Immutable Ledger
- Republicação de eventos pelo contexto proprietário do ledger

---

# Resumo dos Principais Quality Attributes

| Atributo | Estratégia |
|-----------|------------|
| Disponibilidade | Container Apps + Cosmos DB |
| Escalabilidade | Autoscale |
| Desempenho | CQRS |
| Resiliência | Change Feed + Service Bus + Inbox |
| Consistência | Eventual Consistency |
| Auditoria | Immutable Ledger |
| Segurança | Entra ID + Managed Identity |
| Observabilidade | Azure Monitor |
| Operação | IaC |
| Manutenibilidade | Microsserviços + DDD |