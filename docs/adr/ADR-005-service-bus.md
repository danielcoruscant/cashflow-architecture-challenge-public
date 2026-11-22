# ADR-005 - Mensageria com Azure Service Bus

## Status

Aceito

## Contexto

O serviço de consolidação não pode impactar o serviço de lançamentos.

O serviço Consolidado utiliza uma única Subscription do Azure Service Bus. Em cenários de escalabilidade horizontal, múltiplas instâncias do serviço operam no modelo Competing Consumers, garantindo que cada mensagem seja processada por apenas uma instância da aplicação.

## Decisão

Utilizar Azure Service Bus Topic.

Tópico: `lancamentos-topic`

Subscription: `consolidado-sub` (somente o consumidor do Consolidado; nenhum outro consumidor deve usá-la)

Recursos:

- MessageId = `eventId` (rastreabilidade)
- Retry por lock e redelivery, com `maxDeliveryCount` explícito no Terraform
- Conclusão da mensagem somente após o commit da projeção (sem auto-complete)
- Autenticação por Managed Identity (RBAC: sender na Function, receiver no Consolidado)

Não adotado:

- **Duplicate Detection como garantia de negócio:** pode permanecer habilitada como otimização, mas a idempotência é do Inbox Pattern (ADR-012).
- **Tratamento de Dead Letter Queue:** ver ADR-015. Mensagens que esgotam `maxDeliveryCount` vão para a DLQ do sistema do Service Bus e não são reprocessadas nesta entrega.
- **Sessões/ordenação:** a projeção é aditiva e independe da ordem.

## Consequências

### Positivas

- desacoplamento
- tolerância a falhas
- escalabilidade

### Negativas

- consistência eventual
- mensagens esgotadas ficam na DLQ do sistema sem tratamento (risco aceito)