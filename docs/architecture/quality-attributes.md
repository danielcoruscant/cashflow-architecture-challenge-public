# Quality Attributes

## Objetivo

Este documento descreve os principais atributos de qualidade da solução e as decisões arquiteturais adotadas para atendê-los.

A arquitetura foi projetada considerando os requisitos de negócio, requisitos não funcionais e os critérios de avaliação definidos para o desafio. 

---

# Visão Geral

| Atributo | Prioridade | Estratégia |
|-----------|------------|------------|
| Resiliência | Muito Alta | Event Driven + Service Bus |
| Disponibilidade | Muito Alta | Serviços desacoplados |
| Escalabilidade | Alta | Container Apps + Cosmos DB |
| Segurança | Alta | Entra ID + Managed Identity |
| Observabilidade | Alta | Azure Monitor |
| Auditoria | Alta | Immutable Ledger |
| Desempenho | Média | CQRS |
| Manutenibilidade | Alta | DDD + Microsserviços |

---

# QA-01 Resiliência

## Cenário

O serviço de consolidação fica indisponível.

## Resposta Esperada

O serviço de lançamentos continua recebendo e armazenando lançamentos normalmente.

## Decisões Arquiteturais

- Azure Service Bus
- Cosmos Change Feed
- CDC Publisher
- Inbox Pattern

## Benefícios

- isolamento de falhas
- desacoplamento
- redelivery de mensagens pelo broker

## Trade-offs

- consistência eventual
- maior complexidade operacional
- sem replay nem tratamento de DLQ nesta entrega (ADR-009, ADR-015)

---

# QA-02 Disponibilidade

## Cenário

Alta utilização do sistema durante o horário comercial.

## Resposta Esperada

Os serviços permanecem disponíveis e responsivos.

## Decisões Arquiteturais

- Azure Container Apps
- Cosmos DB
- API Management
- Serviços gerenciados Azure

## Meta

Disponibilidade mínima:

99,9%

---

# QA-03 Escalabilidade

## Cenário

O serviço de Consolidado recebe aumento de carga.

## Resposta Esperada

O aumento não afeta o serviço de Lançamentos.

## Decisões Arquiteturais

- CQRS
- Microsserviços
- Azure Container Apps Autoscale
- Cosmos DB Autoscale

## Meta

50 requisições por segundo

com perda máxima de 5% conforme requisito do desafio. 

---

# QA-04 Segurança

## Cenário

Usuário tenta acessar a aplicação.

## Resposta Esperada

A identidade seja validada antes do consumo das APIs.

## Decisões Arquiteturais

- Microsoft Entra ID
- OpenID Connect
- OAuth2
- PKCE
- JWT

## Integrações Internas

- Managed Identity
- Azure RBAC

---

# QA-05 Auditoria

## Cenário

Necessidade de identificar todas as alterações realizadas em determinado dia.

## Resposta Esperada

Todo lançamento deve permanecer disponível para auditoria.

## Decisões Arquiteturais

- Immutable Ledger
- Append Only
- Correções por lançamentos compensatórios

## Benefícios

- rastreabilidade
- histórico completo
- conformidade

---

# QA-06 Observabilidade

## Cenário

Falha ocorre no fluxo de consolidação.

## Resposta Esperada

Localizar rapidamente o ponto de falha.

## Decisões Arquiteturais

- Azure Monitor
- Application Insights
- Log Analytics Workspace

## Métricas

- Requests
- Errors
- Dependencies
- Availability

---

# QA-07 Consistência

## Cenário

Novo lançamento é registrado.

## Resposta Esperada

O saldo consolidado seja atualizado de forma assíncrona.

## Decisão

Consistência Eventual.

## Justificativa

A prioridade do domínio é garantir disponibilidade e resiliência do serviço de lançamentos.

## Trade-off

O saldo consolidado pode levar alguns segundos para refletir a operação.

---

# QA-08 Desempenho

## Cenário

Usuário consulta saldo consolidado.

## Resposta Esperada

Resposta rápida independentemente do volume de lançamentos.

## Decisões Arquiteturais

- CQRS
- Read Model
- Projection Store

## Meta

P95 < 200ms

---

# QA-09 Manutenibilidade

## Cenário

Necessidade de evolução da lógica de consolidação.

## Resposta Esperada

A alteração não deve impactar o serviço de lançamentos.

## Decisões Arquiteturais

- DDD
- Bounded Contexts
- Event Driven Architecture

## Benefícios

- baixo acoplamento
- implantação independente
- evolução isolada

---

# QA-10 Recuperação de Falhas

## Cenário

O serviço de consolidação fica indisponível por determinado período.

## Resposta Esperada

As mensagens sejam processadas após a recuperação, enquanto não esgotarem `maxDeliveryCount`.

## Decisões Arquiteturais

- Azure Service Bus
- Topic
- Retry (lock e redelivery)
- Inbox Pattern

## Limites desta entrega

Mensagens que esgotam `maxDeliveryCount` seguem para a DLQ do sistema sem tratamento, e não há replay (ADR-009, ADR-015). O ledger permanece íntegro para reconstrução futura.

---

# Quality Attribute Trade-offs

## Consistência vs Disponibilidade

### Escolha

Disponibilidade

### Justificativa

O requisito principal estabelece que o serviço de lançamentos não pode ser impactado por falhas no consolidado. 

### Consequência

Consistência eventual.

---

## Simplicidade vs Escalabilidade

### Escolha

Escalabilidade

### Justificativa

A separação entre Lançamentos e Consolidado permite crescimento independente.

### Consequência

Maior número de componentes.

---

## Portabilidade vs Integração Nativa Azure

### Escolha

Integração nativa Azure

### Justificativa

A utilização de Cosmos Change Feed, Managed Identity e Azure Service Bus reduz complexidade e aumenta eficiência operacional.

### Consequência

Maior dependência da plataforma Azure.

---

# Conclusão

A arquitetura proposta prioriza:

1. Resiliência
2. Disponibilidade
3. Segurança
4. Escalabilidade
5. Observabilidade

As decisões arquiteturais adotadas garantem aderência aos requisitos funcionais e não funcionais do desafio, mantendo baixo acoplamento entre serviços e permitindo evolução independente dos domínios de negócio. 
