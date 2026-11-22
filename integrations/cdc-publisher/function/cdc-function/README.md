# CDC Function

Com Java 25 (LTS), compile e empacote localmente com:

```bash
AZURE_FUNCTIONS_APP_NAME=cdc-function-local mvn clean package
```

O nome e exigido pelo plugin para montar o pacote; o empacotamento usa o bundle
de extensoes configurado em `host.json` e nao exige a instalacao local das
extensoes pelo Azure Functions Core Tools.

Para executar a Function localmente, instale o Azure Functions Core Tools.
O host precisa acessar `cdn.functions.azure.com` para baixar o bundle de
extensoes quando necessario.

Caminho do projeto da function:

`integrations/cdc-publisher/function/cdc-function`

## Configuração do destino de publicação

O POM não fixa grupo de recursos, nome de Function App nem região. Para
publicação, configure `AZURE_FUNCTIONS_RESOURCE_GROUP`,
`AZURE_FUNCTIONS_APP_NAME` e `AZURE_FUNCTIONS_REGION` no ambiente Maven. O
comando de empacotamento apenas cria o pacote e não provisiona nem publica
recursos no Azure.

## Autenticacao do trigger no Cosmos DB

O runtime do Azure Functions autentica o trigger por identidade; nao e necessario
criar um cliente Cosmos DB no codigo Java. O bundle de extensoes 4.x configurado
em `host.json` ja suporta conexoes por identidade.

Configure `COSMOS_DATABASE`, `COSMOS_CONTAINER` e `COSMOS_LEASE_CONTAINER` na
Function App. Provisione o container de origem e o container de leases antes de
iniciar a Function; o trigger nao cria containers. O container de leases deve
usar partition key `/id`. Preserve os documentos de lease existentes para
manter os checkpoints do change feed.

Na Function App, habilite a managed identity atribuida pelo sistema e configure:

```text
COSMOS_CONNECTION__accountEndpoint=https://<conta>.documents.azure.com:443/
```

O valor de `connection` no trigger e o prefixo `COSMOS_CONNECTION`. Remova
a configuracao exata `COSMOS_CONNECTION`, se contiver uma connection string:
ela tem precedencia sobre as configuracoes com prefixo e continuaria usando
a chave. Planeje essa remocao junto com a ativacao da identidade e dos acessos.

Para usar uma identidade atribuida pelo usuario, associe-a a Function App e
adicione tambem:

```text
COSMOS_CONNECTION__credential=managedidentity
COSMOS_CONNECTION__clientId=<client-id-da-identidade>
```

Conceda ao **principal ID** da identidade as seguintes funcoes no RBAC de dados
nativo do Cosmos DB para NoSQL:

| Escopo | Funcao |
| --- | --- |
| `/dbs/fluxocaixa/colls/lancamentos` | `Cosmos DB Built-in Data Reader` |
| `/dbs/fluxocaixa/colls/leases` | `Cosmos DB Built-in Data Contributor` |

Roles ID das funcoes de dados do Cosmos DB para NoSQL:

| Funcao | Role ID |
| --- | --- |
| `Cosmos DB Built-in Data Reader` | `00000000-0000-0000-0000-000000000001` |
| `Cosmos DB Built-in Data Contributor` | `00000000-0000-0000-0000-000000000002` |

Comandos para atribuir as funcoes de dados ao principal ID da identidade:

```bash
# Substitua <principal-id> pelo principal ID da identidade
az cosmosdb sql role assignment create \
  --account-name <conta> \
  --resource-group <grupo-de-recursos> \
  --scope "/dbs/fluxocaixa/colls/lancamentos" \
  --role-definition-id 00000000-0000-0000-0000-000000000001 \
  --principal-id <principal-id>

az cosmosdb sql role assignment create \
  --account-name <conta> \
  --resource-group <grupo-de-recursos> \
  --scope "/dbs/fluxocaixa/colls/leases" \
  --role-definition-id 00000000-0000-0000-0000-000000000002 \
  --principal-id <principal-id>
```

O leitor acessa o change feed e o contributor permite atualizar os leases.
Essas funcoes de dados sao distintas das funcoes administrativas do Azure RBAC.
O Service Bus continua exigindo sua propria configuracao e permissao de envio.

Para desenvolvimento local, apos `az login`, configure o endpoint em
`local.settings.json` e conceda as mesmas permissoes de dados a identidade
usada localmente. Omita `credential` e `clientId` para usar as credenciais
locais. Configure tambem o armazenamento do host e o namespace do Service Bus:

```json
{
  "IsEncrypted": false,
  "Values": {
    "AzureWebJobsStorage": "UseDevelopmentStorage=true",
    "FUNCTIONS_WORKER_RUNTIME": "java",
    "COSMOS_DATABASE": "fluxocaixa",
    "COSMOS_CONTAINER": "lancamentos",
    "COSMOS_LEASE_CONTAINER": "leases",
    "COSMOS_CONNECTION__accountEndpoint": "https://<conta>.documents.azure.com:443/",
    "SERVICE_BUS_NAMESPACE": "<namespace>.servicebus.windows.net"
  }
}
```

`UseDevelopmentStorage=true` requer o Azurite em execucao. Nao versione
credenciais nem `local.settings.json`.

Referencias: [trigger do Cosmos DB](https://learn.microsoft.com/azure/azure-functions/functions-bindings-cosmosdb-v2-trigger)
e [funcoes de dados do Cosmos DB](https://learn.microsoft.com/azure/cosmos-db/reference-data-plane-security).

## Testes

Execute `mvn test` para rodar os testes unitarios (JUnit 5 + Mockito).

## Como validar o CDC

1. Confirme que o container `leases` (database `fluxocaixa`) possui os documentos de lease da Function.
2. Crie um documento **novo** no container `lancamentos`, com um `id` inedito.
3. Verifique a mensagem na subscription `consolidado-sub` do topico `lancamentos-topic`.

Pontos de atencao:

- O trigger le o change feed a partir do momento em que o lease e criado pela primeira vez
  (`startFromBeginning` desativado). Documentos criados antes do primeiro start da Function nao sao entregues.
- O topico pode usar duplicate detection (janela de 30 minutos) com o `id` do documento como `MessageId`, apenas como otimizacao.
  A idempotencia de negocio e garantida pelo Inbox Pattern no Consolidado (ADR-012), nao pelo Service Bus.
- O trigger do Cosmos DB nao reentrega um lote que falhou. Por isso a Function falha a invocacao ao final do lote
  e usa retry policy exponencial (5 tentativas), reexecutando o lote de forma idempotente.
- A Function publica um evento de integracao, nao o documento bruto do Cosmos DB. `eventId` e o `id` do documento e
  `correlationId`/`causationId` sao propagados do ledger. O timestamp do evento vem de `_ts`, definido pelo Cosmos DB.
- Os testes cobrem o mapeamento e a configuracao do trigger, mas nao executam Azure Functions Host nem o retry runtime.
