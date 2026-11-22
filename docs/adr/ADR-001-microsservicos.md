# ADR-001 - Arquitetura baseada em Microsserviços

## Status

Aceito

## Contexto

O desafio exige:

- desacoplamento entre o serviço de lançamentos e consolidado
- resiliência
- escalabilidade
- evolução independente dos componentes

## Decisão

Adotar arquitetura baseada em microsserviços.

Serviços:

- Lancamentos Service
- Consolidado Service

## Consequências

### Positivas

- implantação independente
- escalabilidade independente
- isolamento de falhas

### Negativas

- maior complexidade operacional
- necessidade de observabilidade distribuída

## Alternativas Consideradas

### Monolito

Rejeitado devido ao maior acoplamento e dificuldade de evolução.