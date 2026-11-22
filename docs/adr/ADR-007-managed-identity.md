# ADR-007 - Autenticação entre Serviços com Managed Identity

## Status

Aceito

## Contexto

Evitar segredos compartilhados e chaves estáticas.

## Decisão

Utilizar Managed Identity para acesso a:

- Cosmos DB (RBAC de dados por database: `fluxocaixa` para Lançamentos e Function; `consolidado` para o Consolidado)
- Service Bus (sender na Function; receiver no Consolidado)
- Azure Container Registry, Storage de host da Function e demais recursos Azure

## Consequências

### Positivas

- maior segurança
- menor gestão de credenciais
- alinhamento com Azure Well-Architected

### Negativas

- dependência da plataforma Azure