# DDD Context Map

## Objetivo

O objetivo da solução é permitir que um comerciante realize o controle do fluxo de caixa diário através do registro de lançamentos financeiros (créditos e débitos) e da consulta do saldo diário consolidado. A arquitetura foi concebida para atender aos requisitos funcionais e não funcionais descritos no desafio, com foco em escalabilidade, resiliência, segurança, observabilidade e desacoplamento entre serviços. 【1-7ba5eb】

---

# Visão do Domínio

## Core Domain

### Gestão de Fluxo de Caixa

Responsável pela geração de valor para o negócio através do controle financeiro diário.

Capacidades de negócio:

- Registrar lançamentos financeiros
- Consultar lançamentos financeiros
- Calcular saldo diário
- Consultar saldo consolidado
- Disponibilizar relatórios financeiros

---

# Subdomínios

## Core Domain

### Lançamentos Financeiros

Responsável pela gestão das movimentações financeiras.

Capacidades:

- Registrar crédito
- Registrar débito
- Compensar lançamento (estorno por novo lançamento, sem alterar o original)
- Consultar lançamentos

---

### Consolidação Financeira

Responsável pela geração das consultas e relatórios consolidados.

Capacidades:

- Receber eventos financeiros
- Consolidar movimentações
- Calcular saldo diário
- Disponibilizar consultas de saldo

---

## Supporting Domain

### Observabilidade

Capacidades:

- Logs
- Métricas
- Tracing distribuído
- Auditoria

Serviços:

- Azure Application Insights
- Azure Log Analytics

---

## Generic Domain

### Identidade e Acesso

Capacidades:

- Autenticação
- Autorização
- Controle de acesso

Serviço:

- Microsoft Entra ID

---

# Bounded Contexts

## Bounded Context: Lançamentos

### Responsabilidade

Responsável pela gestão transacional das movimentações financeiras.

### Aggregate Root

```text
Lancamento
```

### Entidades

```text
Lancamento
```

### Value Objects

```text
TipoLancamento
ValorMonetario
DataLancamento
```

### Comandos

```text
CriarLancamento
CompensarLancamento
```

Não existem comandos de atualização ou exclusão: o ledger é append-only (ADR-003).

### Eventos de Domínio

```text
LancamentoCriado
```

Uma compensação também gera `LancamentoCriado` (tipo oposto, referenciando o original).

### Persistência

```text
Cosmos DB
Database: fluxocaixa
Container: lancamentos (ledger append-only, partition key /accountId)
Container: leases (Change Feed)
```

### Responsabilidade Exclusiva

Este contexto é o único responsável pelos dados de lançamentos financeiros.

---

## Bounded Context: Consolidado

### Responsabilidade

Responsável pelas consultas consolidadas e projeções financeiras.

### Aggregate Root

```text
ConsolidadoDiario
```

### Entidades

```text
ConsolidadoDiario
```

### Consultas

```text
ObterSaldoDiario
ConsultarConsolidadoPorData
```

### Eventos Consumidos

```text
LancamentoCriado
```

### Persistência

```text
Cosmos DB
Database: consolidado
Container: consolidado-diario (partition key /accountId)
  - saldos diários: id = {accountId}:{yyyy-MM-dd}
  - Inbox: id = eventId
```

### Responsabilidade Exclusiva

Manter uma projeção otimizada para leitura dos saldos consolidados.

---

## Bounded Context: Identity

### Responsabilidade

Autenticação e autorização por operação (JWT, Microsoft Entra ID). Todo usuário autorizado acessa qualquer conta (ADR-017).

### Implementação

```text
Microsoft Entra ID
```

---

# Relacionamentos Entre Contextos

## Lançamentos → Consolidado

Relação:

```text
Upstream / Downstream
```

### Upstream

```text
Lançamentos
```

Publica eventos de integração.

### Downstream

```text
Consolidado
```

Consome eventos e atualiza projeções.

---

# Context Map

```text
+------------------------------------------------+
|                Microsoft Entra ID              |
+------------------------+-----------------------+
                         |
                         |
                         v

+------------------------------------------------+
|          Bounded Context: Lançamentos          |
|------------------------------------------------|
| Comandos                                       |
| - CriarLancamento                              |
| - CompensarLancamento                          |
|                                                |
| Aggregate Root                                 |
| - Lancamento (ledger append-only)              |
+------------------------+-----------------------+
                         |
                         | Cosmos Change Feed
                         | Azure Function (CDC)
                         |
                         v

                 LancamentoCriado

                         |
                         |
                         v

+------------------------------------------------+
|        Azure Service Bus (lancamentos-topic)   |
+------------------------+-----------------------+
                         |
                         |
                         v

+------------------------------------------------+
|          Bounded Context: Consolidado          |
|------------------------------------------------|
| Inbox Pattern (idempotência)                   |
| Queries                                        |
| - ObterSaldoDiario                             |
| - ConsultarConsolidado                         |
|                                                |
| Aggregate Root                                 |
| - ConsolidadoDiario                            |
+------------------------------------------------+
```

---

# Estratégia CQRS

## Write Model

Contexto:

```text
Lançamentos
```

Responsável por:

```text
Commands
```

Exemplos:

```text
CriarLancamento
CompensarLancamento
```

---

## Read Model

Contexto:

```text
Consolidado
```

Responsável por:

```text
Queries
```

Exemplos:

```text
ObterSaldoDiario
ConsultarConsolidadoPorData
```

---

# Eventos de Integração

## LancamentoCriado

Único evento de integração. `eventId` é o `id` do documento no ledger; `correlationId` e `causationId` são gravados no documento pelo Lançamentos e propagados pela Function. Valores em BRL; `data` é a data de negócio (`America/Sao_Paulo`).

```json
{
  "eventId": "uuid",
  "accountId": "C001",
  "data": "2025-06-24",
  "tipo": "CREDITO",
  "valor": 100.00,
  "compensaLancamentoId": null,
  "correlationId": "uuid",
  "causationId": "uuid",
  "timestamp": "2025-06-24T10:00:00Z"
}
```

Compensação: lançamento de tipo oposto, mesmo valor, com `compensaLancamentoId` apontando para o original.

---

# Linguagem Ubíqua

| Termo | Definição |
|---------|------------|
| Lançamento | Registro financeiro imutável realizado pelo comerciante |
| Compensação | Lançamento de tipo oposto que corrige um lançamento anterior |
| Conta (`accountId`) | Identificador da conta; qualquer usuário autorizado acessa qualquer conta (ADR-017) |
| Crédito | Entrada de valor financeiro |
| Débito | Saída de valor financeiro |
| Saldo Diário | Créditos menos débitos de uma conta em uma data de negócio |
| Consolidado Diário | Visão agregada das movimentações da conta no dia |
| Evento Financeiro | Evento publicado após o registro de um lançamento |
| Inbox | Registro de eventos já processados que garante idempotência |
| Fluxo de Caixa | Conjunto das movimentações financeiras do comerciante |

---

# Decisões Arquiteturais

## Separação de Contextos

Os contextos de Lançamentos e Consolidado foram separados para permitir:

- Escalabilidade independente
- Baixo acoplamento
- Evolução isolada
- Persistência especializada
- Resiliência

---

## Comunicação Assíncrona

A integração ocorre por CDC (Cosmos Change Feed), Azure Function e Azure Service Bus; o Outbox Pattern foi descartado (ADR-004). O Consolidado garante idempotência com o Inbox Pattern (ADR-012).

Benefícios:

- Desacoplamento entre serviços
- Tolerância a falhas
- Retry por redelivery
- Consistência eventual

Limites aceitos nesta entrega: sem tratamento de DLQ (ADR-015) e sem replay (ADR-009).

---

## Atendimento aos Requisitos Não Funcionais

A separação dos contextos garante que o serviço de lançamentos permaneça disponível mesmo em caso de indisponibilidade do serviço de consolidação, conforme requisito do desafio. Também permite escalar o contexto de consolidação para suportar os picos de consumo esperados.

Além disso, a comunicação assíncrona contribui para a resiliência do sistema, permitindo que falhas temporárias não impactem diretamente a operação dos serviços e garantindo a consistência eventual dos dados.