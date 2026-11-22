# ADR-004 - Publicação de Eventos via Cosmos Change Feed

## Status

Aceito

## Contexto

Necessidade de publicar eventos financeiros sem impactar o serviço de lançamentos.

## Decisão

Utilizar:

- Cosmos DB Change Feed
- Azure Functions
- CDC Publisher

Para publicação dos eventos de integração.

Fluxo:

```text
Ledger (Cosmos DB) → Change Feed → Azure Function → Service Bus (lancamentos-topic)
```

Regras:

- O ledger append-only é a única fonte de verdade; nenhum evento é persistido em container auxiliar.
- Cada documento do ledger gera um único evento `LancamentoCriado`; `eventId` é o `id` do documento.
- `correlationId` e `causationId` são gravados no documento pelo Lançamentos e apenas propagados pela Function.
- O container `leases` é provisionado previamente (Terraform) e a Function não o cria.
- A idempotência do consumo é responsabilidade do Inbox Pattern (ADR-012), não do Service Bus.
- O trigger não reentrega um lote que falhou; a Function falha a invocação e usa retry exponencial. Após esgotar os retries, o lote pode deixar de ser publicado; sem replay (ADR-009) a divergência não é corrigida automaticamente.

## Consequências

### Positivas

- menor acoplamento
- menor complexidade na aplicação
- sem armazenamento duplicado de eventos
- aproveitamento de recurso nativo Azure

### Negativas

- dependência do ecossistema Azure
- sem garantia de reentrega após esgotar os retries da Function
- o evento é derivado do documento: mudanças de esquema do ledger afetam o contrato do evento

## Alternativas

Outbox Pattern

**Descartado.** O Change Feed elimina o armazenamento duplicado de eventos, reduz a complexidade e mantém o desacoplamento entre os contextos.