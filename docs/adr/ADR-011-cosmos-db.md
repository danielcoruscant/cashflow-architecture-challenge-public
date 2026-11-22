# ADR-011 – Escolha do Azure Cosmos DB como Banco de Dados Principal

> Renumerada de "ADR-001" (numeração duplicada com a ADR-001 de microsserviços). Revisada conforme o [plano de implementação](../implementation/implementation-plan.md): o identificador de conta passa a ser `accountId` (antes `contaId`), a integração usa CDC + Inbox e o ledger é append-only.

## Status

Aprovado

## Data

Junho/2025

---

## Contexto

O projeto tem como objetivo disponibilizar uma solução para controle de fluxo de caixa diário de um comerciante, permitindo o registro de débitos e créditos e a geração de um saldo consolidado diário. A solução deve contemplar requisitos de escalabilidade, resiliência, disponibilidade e desacoplamento entre os serviços de lançamentos e consolidação. 

Entre os requisitos não funcionais definidos, destaca-se que o serviço de lançamentos deve continuar disponível mesmo que o serviço de consolidado diário esteja indisponível. Além disso, o serviço de consolidado deve suportar picos de até 50 requisições por segundo com perda máxima de 5% das requisições. 

A arquitetura proposta é baseada em microsserviços, onde:

- O Serviço de Lançamentos é responsável pela persistência dos débitos e créditos.
- O Serviço de Consolidado Diário é responsável pela apuração e disponibilização dos saldos consolidados.
- Os serviços devem possuir baixo acoplamento e capacidade de escalar independentemente. 

---

## Decisão

Foi escolhido o **Azure Cosmos DB** como banco de dados principal da solução.

Adicionalmente, foi definido o atributo **`accountId`** como **Partition Key** do container de lançamentos (`/accountId`) e do container do read model do Consolidado (`consolidado-diario`).

A conta Cosmos DB é única, com dois databases, um por contexto delimitado, para impedir que um serviço acesse o banco do outro e permitir RBAC de dados por database:

| Database | Contexto | Containers |
|---|---|---|
| `fluxocaixa` | Lançamentos | `lancamentos` (ledger, `/accountId`), `leases` (`/id`) |
| `consolidado` | Consolidado | `consolidado-diario` (`/accountId`): saldos diários e Inbox |

---

# Justificativa da Escolha do Cosmos DB

## 1. Escalabilidade Horizontal Nativa

Um dos principais requisitos do desafio é a capacidade de escalar horizontalmente sem degradação significativa de desempenho. 

O Cosmos DB fornece:

- Particionamento automático.
- Distribuição automática dos dados.
- Escalabilidade horizontal transparente.
- Throughput configurável e autoscale.
- Replicação geográfica nativa.

Diferentemente de bancos relacionais tradicionais, não há necessidade de implementar manualmente estratégias de sharding ou gerenciar clusters complexos.

---

## 2. Alta Disponibilidade e Resiliência

O desafio enfatiza requisitos de disponibilidade e recuperação de falhas. 

O Cosmos DB fornece recursos nativos que atendem esses requisitos:

- Replicação automática.
- Failover gerenciado.
- Distribuição entre zonas de disponibilidade.
- SLA elevado para disponibilidade e baixa latência.

Esses recursos reduzem significativamente o esforço operacional e aumentam a resiliência da plataforma.

---

## 3. Integração Natural com Arquitetura Orientada a Eventos

A solução proposta utiliza uma comunicação assíncrona entre os domínios de Lançamentos e Consolidado.

```text
API de Lançamentos
        │
        ▼
 Cosmos DB (ledger append-only)
        │
   Change Feed
        │
        ▼
 Azure Function (CDC Publisher)
        │
        ▼
 Azure Service Bus
        │
        ▼
 Serviço de Consolidado (Inbox + projeção)
```

O recurso **Change Feed** do Cosmos DB permite capturar os lançamentos gravados sem impactar a operação principal (ver ADR-004). O Outbox Pattern foi descartado.

Benefícios:

- Desacoplamento entre serviços.
- Escalabilidade independente.
- Processamento assíncrono.
- Redução do impacto de falhas entre domínios.
- Melhor aderência aos requisitos de resiliência. 

---

## 4. Flexibilidade para Evolução do Modelo

Os registros financeiros possuem estrutura naturalmente orientada a documentos:

```json
{
  "id": "123",
  "accountId": "C001",
  "tipo": "CREDITO",
  "valor": 100.00,
  "data": "2025-06-15",
  "correlationId": "…",
  "causationId": "…"
}
```

O Cosmos DB possibilita:

- Evolução de schema sem migrações complexas.
- Inclusão de novos atributos de negócio.
- Armazenamento eficiente de documentos JSON.
- Menor impacto em futuras evoluções do domínio.

---

# Consistência Eventual

A arquitetura adota o conceito de **Eventual Consistency** entre os serviços de lançamentos e consolidado.

Nesse modelo, após a gravação de um lançamento, o dado é imediatamente persistido no Cosmos DB, porém a atualização do saldo consolidado ocorre de forma assíncrona através do Change Feed.

Exemplo:

```text
10:00:00 -> Crédito registrado
10:00:01 -> Saldo consolidado ainda não atualizado
10:00:02 -> Evento processado
10:00:03 -> Saldo atualizado
```

Durante um curto intervalo de tempo, a leitura do consolidado pode apresentar dados ligeiramente defasados.

Essa abordagem foi considerada aceitável porque:

- O lançamento é a fonte oficial da verdade.
- O consolidado possui característica analítica.
- Os serviços permanecem desacoplados.
- Uma falha do consolidado não impacta o registro de novos lançamentos. 

O ganho em disponibilidade e resiliência compensa a pequena janela de inconsistência temporária.

---

# Alternativas Avaliadas

## Azure Table Storage

### Prós

- Baixo custo.
- Simplicidade operacional.
- Boa performance para consultas baseadas em chave.

### Contras

- Capacidades de consulta limitadas.
- Menor flexibilidade para evolução do modelo.
- Recursos mais limitados de indexação.
- Ausência de funcionalidades equivalentes ao Change Feed do Cosmos DB.
- Menor aderência a arquiteturas orientadas a eventos.

### Motivo da Não Escolha

Embora apresente menor custo inicial, o Table Storage impõe limitações funcionais e arquiteturais que podem aumentar a complexidade de evolução da solução ao longo do tempo.

---

## SQL Server

### Prós

- Consistência transacional forte.
- Suporte avançado a relacionamentos.
- Ecossistema maduro.

### Contras

- Escalabilidade horizontal mais complexa.
- Maior dependência de estratégias de particionamento.
- Maior esforço operacional para suportar crescimento.

### Motivo da Não Escolha

O problema em questão não apresenta relacionamentos complexos que justifiquem os benefícios de um banco relacional tradicional. A escalabilidade horizontal oferecida pelo Cosmos DB é mais aderente aos requisitos do desafio. 

---

## PostgreSQL

### Prós

- Open Source.
- Excelente desempenho.
- Suporte a JSON (JSONB).
- Forte consistência transacional.

### Contras

- Necessidade de administração de replicação.
- Particionamento mais complexo.
- CDC e integração orientada a eventos exigem soluções complementares.

### Motivo da Não Escolha

Embora seja tecnicamente capaz de resolver o problema, exige maior esforço operacional para atingir níveis equivalentes de escalabilidade e integração orientada a eventos.

---

## MySQL

### Prós

- Simplicidade.
- Grande adoção de mercado.
- Baixo custo.

### Contras

- Escalabilidade horizontal limitada.
- Necessidade de estratégias complementares de sharding.
- Integração orientada a eventos menos nativa.

### Motivo da Não Escolha

Atende ao requisito funcional, porém demanda maior esforço arquitetural para alcançar os mesmos atributos de escalabilidade e resiliência.

---

# Escolha da Partition Key

## Decisão

```json
{
  "partitionKey": "/accountId"
}
```

---

## Justificativa

### Alinhamento ao Domínio

A conta é o principal agregado de negócio da solução.

Todo lançamento pertence obrigatoriamente a uma conta e a maioria das consultas ocorre dentro desse contexto.

---

### Consultas Mais Frequentes

As consultas previstas incluem:

```text
Obter lançamentos da conta
Consultar saldo da conta
Consolidar movimentações da conta
Extrair histórico da conta
```

Utilizando `accountId` como Partition Key:

- Os dados relacionados permanecem agrupados.
- Reduz-se a necessidade de consultas cross-partition.
- Diminui-se o consumo de Request Units (RU).
- Melhora-se a latência das consultas.

---

### Escalabilidade

À medida que novas contas são criadas, os dados são distribuídos automaticamente entre as partições físicas do Cosmos DB.

```text
Conta A → Partição Física 1
Conta B → Partição Física 2
Conta C → Partição Física 3
Conta D → Partição Física 4
```

Isso permite crescimento horizontal sem necessidade de reestruturações futuras.

---

### Eficiência no Consolidado Diário

Como os cálculos do consolidado costumam ocorrer por conta, manter todos os lançamentos da mesma conta na mesma partição reduz operações distribuídas e melhora a eficiência do processamento.

---

# Consequências

## Positivas

- Escalabilidade horizontal nativa.
- Alta disponibilidade.
- Menor esforço operacional.
- Excelente integração com arquitetura orientada a eventos.
- Fácil evolução do modelo de dados.
- Atendimento aos requisitos de resiliência e desacoplamento dos serviços. 

## Negativas

- Custo superior ao Azure Table Storage.
- Necessidade de monitorar consumo de RU.
- Consultas cross-partition podem aumentar custos caso novos requisitos não sejam modelados adequadamente.
- Contas de altíssimo volume podem gerar *hot partition* e esbarrar no limite de 20 GB por partição lógica; o ledger é append-only e cresce continuamente.
- `TransactionalBatch` só opera dentro de uma partição lógica: a Inbox e o saldo diário do Consolidado compartilham o mesmo container e a mesma chave `accountId` (ADR-012).

---

# Conclusão

O Azure Cosmos DB foi selecionado como banco de dados principal por apresentar a melhor aderência aos requisitos de escalabilidade, disponibilidade, resiliência e desacoplamento definidos para o projeto. Sua integração nativa com arquiteturas orientadas a eventos, através do Change Feed, permite implementar uma estratégia de consistência eventual entre os serviços de lançamento e consolidado, garantindo que falhas no consolidado não afetem a operação principal do negócio. A utilização de `accountId` como Partition Key maximiza a eficiência das consultas, reduz custos operacionais e viabiliza o crescimento horizontal da solução de forma transparente. 