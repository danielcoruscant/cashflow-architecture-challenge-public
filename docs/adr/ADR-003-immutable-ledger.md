# ADR-003 - Persistência Imutável dos Lançamentos

## Status

Aceito

## Contexto

O domínio financeiro exige rastreabilidade e auditoria.

## Decisão

Persistir lançamentos em modelo append-only.

Não permitir:

- update
- delete

Correções serão realizadas através de lançamentos compensatórios.

### Regras do ledger

- Cada lançamento é um documento imutável, identificado por `id` e pela conta (`accountId`), com valor em BRL e data de negócio no fuso `America/Sao_Paulo`.
- A compensação é um novo lançamento de tipo oposto, com o mesmo valor, que referencia o lançamento original (`compensaLancamentoId`).
- As APIs não oferecem atualização nem exclusão; não existem os eventos `LancamentoAtualizado` e `LancamentoExcluido`.
- O ledger é a única fonte de verdade; não há container de Outbox (ver ADR-004).

## Consequências

### Positivas

- auditoria completa
- rastreabilidade
- reprocessamento de projeções

### Negativas

- crescimento contínuo da base
- necessidade de políticas de retenção futuras

## Alternativas

CRUD tradicional

Rejeitado pela perda de histórico.