# ADR-008 - Estratégia de Observabilidade

## Status

Aceito

## Contexto

O desafio cita monitoramento e observabilidade como requisito diferencial.

## Decisão

Para esta entrega, utilizar:

- Azure Monitor
- Application Insights
- Log Analytics Workspace
- Logs estruturados com `correlationId` e `causationId` propagados da API ao ledger, ao evento e ao consumidor

O agente Java do Application Insights é carregado nas imagens dos serviços
Spring. A execução local de testes não comprova ingestão de telemetria no Azure.
OpenTelemetry e tracing distribuído por spans ficam como evolução futura.

## Consequências

### Positivas

- monitoramento centralizado
- troubleshooting simplificado
- auditoria operacional

### Negativas

- custo adicional de retenção de logs
