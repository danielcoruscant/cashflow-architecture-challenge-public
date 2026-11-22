# ADR-015 - Exclusão do Tratamento de Dead Letter Queue

## Status

Aceito

## Contexto

O tratamento operacional de DLQ (monitoramento, análise, correção e reenvio) exige processos e ferramentas que agregam esforço desproporcional ao valor demonstrável no desafio.

## Decisão

Não implementar tratamento de Dead Letter Queue nesta entrega.

O consumidor do Consolidado aplica retry por redelivery (abandono da mensagem e lock) e conclui a mensagem apenas após o commit. A entidade Service Bus continua com um `maxDeliveryCount` explícito definido no Terraform.

## Consequências

### Positivas

- Menor complexidade operacional e de código.

### Negativas

- Mensagens que esgotam `maxDeliveryCount` são movidas pelo Service Bus para a DLQ do sistema e não são reprocessadas automaticamente. O saldo permanece divergente até haver replay (ADR-009).
- Não há alerta nem runbook de DLQ nesta entrega; o backlog da DLQ do sistema pode ser consultado manualmente.

## Evolução futura

Recomendado para produção: alertas sobre a DLQ, runbook de análise, reenvio controlado e integração com o Replay.
