# ADR-002 - Separação CQRS

## Status

Aceito

## Contexto

O consolidado representa um modelo de leitura derivado dos lançamentos.

A consulta do saldo diário possui características distintas das operações de escrita.

## Decisão

Separar:

Write Model

- Lancamentos Service

Read Model

- Consolidado Service

## Consequências

### Positivas

- leitura otimizada
- escalabilidade independente
- menor acoplamento

### Negativas

- consistência eventual