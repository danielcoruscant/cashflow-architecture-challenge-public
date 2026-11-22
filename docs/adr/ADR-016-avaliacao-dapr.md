# ADR-016 - Avaliação do Dapr

## Status

Aceito (Dapr não adotado)

## Contexto

O fluxo CDC → Function → Service Bus → Consolidado é simples e usa SDKs Azure diretamente, com recursos específicos do Cosmos DB (Change Feed, `TransactionalBatch`, ETag).

## Decisão

Não utilizar o Dapr nesta entrega.

## Consequências

### Positivas

- Menos componentes, sidecars e pontos de falha.
- Acesso direto aos recursos nativos do Cosmos DB e do Service Bus.

### Negativas

- Maior acoplamento com os SDKs Azure.

## Não recomendado

Usar o Dapr como abstração principal do Cosmos DB quando a solução depende de `TransactionalBatch`, Change Feed, ETag e Inbox Pattern.

## Evolução futura

Avaliar Pub/Sub, Service Invocation e observabilidade distribuída.
