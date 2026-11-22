# Serviço de Lançamentos

API Java 25/Spring Boot do ledger append-only. Os contratos estão em `contracts/lancamentos-api/openapi.yaml`.

## Configuração

Defina estas variáveis antes de iniciar a aplicação:

| Variável | Finalidade |
|---|---|
| `COSMOS_ENDPOINT` | Endpoint HTTPS da conta Cosmos DB |
| `COSMOS_DATABASE` | Database do ledger (padrão: `fluxocaixa`) |
| `JWT_ISSUER_URI` | Emissor esperado nos access tokens |
| `JWT_JWK_SET_URI` | Endpoint JWK usado para validar assinatura |
| `JWT_AUDIENCE` | Audience esperada no token |

O acesso Cosmos usa `DefaultAzureCredential`; não configure chave ou connection string no código. Use `az login` para desenvolvimento local e conceda a permissão de dados necessária. A API exige JWT com `lancamentos.read` para consultas e `lancamentos.write` para criação/compensação.

## Build e testes

Na raiz do repositório:

```bash
mvn -B -ntp -f services/lancamentos-service/pom.xml clean verify
```

O comando compila, gera as interfaces a partir do OpenAPI, executa testes unitários/contratuais/de contexto e produz relatório JaCoCo. Para executar também o teste de integração do repositório com o Cosmos Emulator via Testcontainers:

```bash
mvn -B -ntp -f services/lancamentos-service/pom.xml -Dtest=CosmosEmulatorIT test
```

Esse teste baixa a imagem Linux vNext se necessário e verifica gravação, leitura e isolamento da chave de partição. O Emulator vNext suporta apenas a API NoSQL em gateway mode, não aplica autenticação e implementa apenas um subconjunto de recursos; o teste não comprova autenticação, RBAC ou conectividade com Cosmos DB no Azure.

## Observabilidade

A imagem Docker carrega o agente Java do Application Insights e a aplicação emite logs JSON estruturados. A configuração Terraform fornece `APPLICATIONINSIGHTS_CONNECTION_STRING` ao Container App. A execução Maven local não carrega o agente; testes verificam os campos de log e `correlationId`, mas não comprovam ingestão no Application Insights.

## Execução local

Após configurar as variáveis e uma credencial Azure suportada:

```bash
cd services/lancamentos-service
mvn spring-boot:run
```

A API usa `/api/v1`; probes de liveness/readiness ficam em `/actuator/health/liveness` e `/actuator/health/readiness`. Não exponha os endpoints de probes publicamente.
