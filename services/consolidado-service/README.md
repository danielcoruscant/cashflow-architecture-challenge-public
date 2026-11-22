# Serviço de Consolidado

API Java 25/Spring Boot de consulta do read model diário e consumidor do tópico de lançamentos. O contrato HTTP está em `contracts/consolidado-api/openapi.yaml`.

## Configuração

Defina estas variáveis antes de iniciar a aplicação:

| Variável | Finalidade |
|---|---|
| `COSMOS_ENDPOINT` | Endpoint HTTPS da conta Cosmos DB |
| `COSMOS_DATABASE` | Database do read model (padrão: `consolidado`) |
| `SERVICE_BUS_NAMESPACE` | Namespace, no formato `<namespace>.servicebus.windows.net` |
| `SERVICE_BUS_TOPIC` | Tópico de eventos (padrão: `lancamentos-topic`) |
| `SERVICE_BUS_SUBSCRIPTION` | Subscription consumida (padrão: `consolidado-sub`) |
| `JWT_ISSUER_URI` | Emissor esperado nos access tokens |
| `JWT_JWK_SET_URI` | Endpoint JWK usado para validar assinatura |
| `JWT_AUDIENCE` | Audience esperada no token |

Cosmos DB e Service Bus usam `DefaultAzureCredential`; não configure connection strings nem chaves no código. Use `az login` para desenvolvimento local e conceda a permissão de dados mínima. A API exige o escopo `consolidado.read`.

## Build e testes

Na raiz do repositório:

```bash
mvn -B -ntp -f services/consolidado-service/pom.xml clean verify
```

O comando compila, gera interfaces a partir do OpenAPI, executa testes unitários/contratuais/de contexto e produz relatório JaCoCo. Para executar também a integração de batch e Inbox com o Cosmos Emulator via Testcontainers:

```bash
mvn -B -ntp -f services/consolidado-service/pom.xml -Dtest=CosmosEmulatorIT test
```

Esse teste baixa a imagem Linux vNext se necessário e verifica atualização atômica do saldo, idempotência de evento duplicado e rejeição de atualização com ETag obsoleto. O Emulator vNext suporta apenas a API NoSQL em gateway mode, não aplica autenticação e implementa apenas um subconjunto de recursos; o teste não comprova autenticação, RBAC ou conectividade com Cosmos DB no Azure, nem integração com Service Bus real.

## Observabilidade

A imagem Docker carrega o agente Java do Application Insights e a aplicação emite logs JSON estruturados. A configuração Terraform fornece `APPLICATIONINSIGHTS_CONNECTION_STRING` ao Container App. A execução Maven local não carrega o agente; testes verificam os campos de log e `correlationId`, mas não comprovam ingestão no Application Insights.

## Execução local

Após configurar as variáveis e uma credencial Azure suportada:

```bash
cd services/consolidado-service
mvn spring-boot:run
```

A API usa `/api/v1`; probes de liveness/readiness ficam em `/actuator/health/liveness` e `/actuator/health/readiness`. Não exponha os endpoints de probes publicamente.
