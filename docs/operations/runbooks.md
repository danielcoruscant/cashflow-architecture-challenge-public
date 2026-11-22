# Runbooks operacionais

Este documento descreve verificações operacionais sem executar replay, republicação ou tratamento de DLQ. Esses recursos estão fora do escopo atual, conforme os ADRs-009 e -015.

## Resiliência de chamadas síncronas

**T15: N/A para Resilience4j.** Não existem clientes síncronos HTTP/REST ou chamadas síncronas entre serviços. Os clientes síncronos presentes são `CosmosClient` nos dois serviços e `ServiceBusSenderClient` no publisher; o consumidor usa `ServiceBusProcessorClient` por callbacks. Os SDKs Azure aplicam suas políticas nativas de retry, e o publisher também está sujeito ao retry do host da Function. Não se adiciona uma segunda camada genérica de retry/circuit breaker sobre esses clientes para evitar duplicar tentativas e ampliar o tempo de processamento; não há política Resilience4j própria a testar.

O cliente de descoberta de chaves JWK é parte do Resource Server Spring Security, não uma chamada de negócio entre serviços. A recuperação de chaves segue o comportamento da biblioteca e não é substituída por uma política Resilience4j local.

## Reinício da Azure Function Publisher

1. Verifique a saúde do Function App e a identidade gerenciada associada.
2. Confirme que o Change Feed Processor usa os containers e a conta esperados.
3. Confirme que o lease container foi pré-provisionado com a partition key `/id`; a Function não cria o container.
4. Reinicie o Function App pelo mecanismo operacional aprovado e acompanhe os logs de inicialização e publicação.
5. Compare a evolução do Change Feed com os eventos publicados no tópico e acompanhe falhas/retries nos logs.
6. Se a publicação não se recuperar, preserve os logs e escale para investigação. Não recrie leases nem republique eventos manualmente: replay/reconstrução não está implementado.

## Reinício do consumidor Consolidado

1. Verifique o estado da Container App, identidade gerenciada e permissões de receiver no Service Bus.
2. Confira o estado da subscription, quantidade de mensagens ativas, dead-letter e tentativas observadas nos logs.
3. Reinicie a aplicação pelo mecanismo operacional aprovado e confirme a inicialização do processor.
4. Verifique que mensagens processadas atualizam a Inbox e o consolidado atomicamente e que duplicatas não alteram o saldo.
5. Se o backlog ou erros persistirem, registre o `eventId`, `correlationId`, contagem de entrega e logs relevantes para investigação.
6. Não remova mensagens da DLQ, não as reenvie e não altere a Inbox manualmente; o tratamento de DLQ e replay não estão implementados.
