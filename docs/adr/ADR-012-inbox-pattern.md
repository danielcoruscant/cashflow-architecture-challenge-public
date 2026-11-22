# ADR-012 - Inbox Pattern para Idempotência do Consolidado

## Status

Aceito

## Contexto

O Service Bus entrega mensagens com semântica *at-least-once*. O Consolidado precisa evitar a aplicação duplicada de um evento ao saldo diário. A Duplicate Detection do Service Bus atua apenas na janela configurada e no momento do envio; não protege contra redelivery nem contra reenvio fora da janela.

## Decisão

Adotar o **Inbox Pattern** como mecanismo principal de idempotência:

- Cada evento é identificado por `eventId` (igual ao `id` do lançamento no ledger).
- A Inbox e o saldo diário residem no mesmo container (`consolidado-diario`) e na mesma partição lógica (`/accountId`).
- O processamento usa `TransactionalBatch`: *create* do documento de Inbox (`id = eventId`) e *replace* do saldo diário com `If-Match` (ETag).
- Conflito `409` na Inbox indica duplicata: a mensagem é concluída sem alterar o saldo.
- Falha de ETag provoca nova leitura e retry limitado.
- A mensagem é concluída no Service Bus somente após o commit do batch.
- A consistência de negócio **não** depende da Duplicate Detection do Service Bus.

## Consequências

### Positivas

- Idempotência garantida pelo armazenamento, mesmo com redelivery e reprocessamento.
- Atualização atômica de Inbox e projeção.
- Projeção aditiva: a ordem de chegada não altera o resultado.

### Negativas

- Crescimento da Inbox; retenção deve ser compatível com a janela de redelivery e com um replay futuro.
- Contenção de ETag em contas de alto volume.
- Inbox e projeção compartilham partição, o que condiciona o modelo de dados.

## Alternativas

- **Duplicate Detection do Service Bus:** insuficiente como garantia de negócio.
- **Projeção naturalmente idempotente (recalcular a partir do ledger):** exigiria acesso ao banco de outro contexto.
