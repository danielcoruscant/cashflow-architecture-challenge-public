# Risk Analysis and Mitigations

## Objetivo

Este documento descreve os principais riscos técnicos, arquiteturais e operacionais identificados durante o desenho da solução, bem como as estratégias de mitigação adotadas.

A análise foi baseada nos requisitos de negócio, requisitos não funcionais e atributos de qualidade definidos para a solução. 【1-0cde3f】

---

# Matriz de Riscos

| ID | Risco | Probabilidade | Impacto | Criticidade |
|------|---------|--------------|----------|-------------|
| R01 | Indisponibilidade do serviço de Consolidação | Média | Alto | Alto |
| R02 | Perda de eventos financeiros | Baixa | Muito Alto | Alto |
| R03 | Processamento duplicado de eventos | Média | Alto | Alto |
| R04 | Aumento de volume de lançamentos | Média | Médio | Médio |
| R05 | Crescimento do Cosmos DB | Alto | Médio | Médio |
| R06 | Falha do CDC Publisher | Média | Alto | Alto |
| R07 | Vazamento de credenciais | Baixa | Muito Alto | Alto |
| R08 | Falhas de observabilidade | Média | Médio | Médio |
| R09 | Erro no modelo de consolidação | Média | Alto | Alto |
| R10 | Dependência excessiva da Azure | Alta | Médio | Médio |

---

# R01 - Indisponibilidade do Serviço de Consolidação

## Cenário

O serviço de Consolidado fica indisponível devido a falha de aplicação ou infraestrutura.

## Impacto

Os saldos diários deixam de ser atualizados temporariamente.

## Mitigação

- Azure Service Bus Topic
- Processamento assíncrono
- Inbox Pattern
- Retry por redelivery

## Resultado Esperado

O serviço de Lançamentos continua operando normalmente. Os eventos pendentes são processados após a recuperação, enquanto não esgotarem `maxDeliveryCount`; caso esgotem, seguem para a DLQ do sistema sem tratamento nesta entrega (ADR-015). 【1-0cde3f】

---

# R02 - Perda de Eventos Financeiros

## Cenário

Evento financeiro não chega ao serviço de consolidação (por exemplo, a Function esgota os retries do lote ou a mensagem esgota `maxDeliveryCount`).

## Impacto

Desalinhamento do saldo consolidado.

## Mitigação

- Cosmos Change Feed
- Service Bus
- Persistência da origem (ledger append-only) no Cosmos DB
- Monitoramento de falhas da Function e do backlog

## Resultado Esperado

A movimentação nunca é perdida do ledger. A reconstrução automática do consolidado (replay) **não é implementada nesta entrega** (ADR-009); a divergência permanece até a evolução futura. **Risco residual aceito.**

---

# R03 - Processamento Duplicado

## Cenário

A mesma mensagem é entregue mais de uma vez.

## Impacto

Duplicidade no saldo consolidado.

## Mitigação

- Inbox Pattern (mecanismo principal, ADR-012)
- TransactionalBatch (Inbox + projeção atômicos)
- ETag para concorrência
- MessageId = eventId (rastreabilidade)

## Resultado Esperado

Processamento idempotente.

---

# R04 - Crescimento da Carga

## Cenário

Aumento da quantidade de lançamentos ou consultas.

## Impacto

Degradação de desempenho.

## Mitigação

- Azure Container Apps Autoscale
- Cosmos DB Autoscale
- CQRS
- Read Model separado

## Resultado Esperado

Escalabilidade horizontal sem indisponibilidade.

---

# R05 - Crescimento do Ledger

## Cenário

A natureza append-only aumenta continuamente o volume de dados.

## Impacto

Aumento de custos e utilização de armazenamento.

## Mitigação

- Particionamento adequado
- Lifecycle Management
- Arquivamento futuro
- Monitoramento de consumo

## Resultado Esperado

Escalabilidade controlada do armazenamento.

---

# R06 - Falha do CDC Publisher

## Cenário

Azure Function responsável pela publicação de eventos fica indisponível.

## Impacto

Novos eventos deixam de ser enviados ao barramento.

## Mitigação

- Cosmos Change Feed é persistente (leases pré-provisionados)
- Azure Functions Serverless
- Retry exponencial da invocação
- Monitoramento via Application Insights

## Resultado Esperado

Recuperação automática quando a Function volta dentro dos retries. Após esgotar os retries o lote pode não ser publicado e não há replay (ADR-009): **risco residual aceito**.

---

# R07 - Comprometimento de Credenciais

## Cenário

Exposição de segredos ou chaves de acesso.

## Impacto

Acesso indevido a recursos da solução.

## Mitigação

- Managed Identity
- Azure RBAC
- Ausência de credenciais em código
- Controle centralizado de acesso

## Resultado Esperado

Redução da superfície de ataque.

---

# R08 - Falhas de Observabilidade

## Cenário

Exceções ou falhas não são registradas adequadamente.

## Impacto

Aumento do tempo para diagnóstico.

## Mitigação

- Application Insights
- Log Analytics Workspace
- Alertas
- Dashboards operacionais

## Resultado Esperado

Investigação rápida de incidentes.

---

# R09 - Erro na Lógica de Consolidação

## Cenário

Bug na projeção do saldo consolidado.

## Impacto

Exibição incorreta dos saldos.

## Mitigação

- Testes automatizados
- Testes de integração
- Testes de contrato
- Ledger íntegro como fonte para correção futura

## Resultado Esperado

Correção do código sem perda de histórico. A reconstrução da projeção a partir do ledger é evolução futura (ADR-009).

---

# R10 - Dependência da Plataforma Azure

## Cenário

A solução utiliza recursos específicos da Azure.

## Impacto

Maior complexidade em eventual migração.

## Mitigação

- Containers padronizados
- APIs REST
- Domain Layer desacoplada
- Documentação arquitetural

## Resultado Esperado

Preservação da lógica de negócio mesmo em cenários de migração futura.

---

# Análise dos Principais Trade-offs

## Disponibilidade vs Consistência

### Escolha

Disponibilidade

### Justificativa

Os lançamentos não podem ficar indisponíveis devido a falhas do consolidado. 【1-0cde3f】

### Consequência

Consistência eventual no saldo consolidado.

---

## Simplicidade vs Resiliência

### Escolha

Resiliência

### Justificativa

Service Bus, CDC e CQRS adicionam complexidade, porém aumentam significativamente a robustez da solução.

---

## Custo vs Escalabilidade

### Escolha

Escalabilidade

### Justificativa

A utilização de serviços gerenciados permite crescimento sustentável da plataforma.

---

## Portabilidade vs Produtividade

### Escolha

Produtividade

### Justificativa

Recursos nativos como:

- Cosmos Change Feed
- Managed Identity
- Service Bus
- Application Insights

reduzem a complexidade operacional.

---

# Plano de Resposta a Incidentes

| Incidente | Resposta |
|------------|-----------|
| Falha no Consolidado | Reiniciar o consumidor; mensagens pendentes são redeliveradas |
| Falha no CDC | Reiniciar a Function; o Change Feed retoma pelos leases |
| Erro de projeção | Corrigir o código; reconstrução do Read Model é evolução futura (ADR-009) |
| Falha em APIs | Escalabilidade automática |
| Falha de autenticação | Logs e diagnóstico via Entra ID |
| Falha de integração | Retry por redelivery; DLQ do sistema sem tratamento (ADR-015) |

---

# Conclusão

A arquitetura foi projetada priorizando:

1. Resiliência
2. Disponibilidade
3. Segurança
4. Observabilidade
5. Escalabilidade

Os principais riscos foram mitigados através do uso de padrões como:

- DDD
- CQRS
- Event Driven Architecture
- Immutable Ledger
- Cosmos Change Feed
- Inbox Pattern
- Managed Identity

garantindo aderência aos requisitos funcionais e não funcionais descritos no desafio. 【1-0cde3f】