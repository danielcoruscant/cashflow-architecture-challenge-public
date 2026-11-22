# Guia de implantação produtiva no Azure

## Status e limites

Este é um procedimento planejado para uma futura implantação; não comprova
prontidão, conectividade ou operação em Azure. A composição Terraform mantida é
`infra/environments/prod`, mas nenhum `terraform plan` autenticado, `apply` ou
deploy foi executado. Não execute as etapas de provisionamento até fechar os
bloqueios abaixo e obter aprovação explícita de segurança e operações.

### Bloqueios para declarar produção

| Bloqueio | Situação atual / ação necessária |
|---|---|
| Gate de segurança | O relatório de evidências registra dois achados CRITICAL em `spring-webmvc 6.2.19` nas imagens. Corrigir ou obter aceitação formal do risco antes de publicar. |
| SLA do gateway | O APIM está configurado como `Developer_1`, sem SLA. Escolher e validar um tier com SLA compatível com a topologia VNet interna. |
| Fronteira de rede | Cosmos DB, Service Bus, ACR e Function têm acesso público habilitado na composição atual. Definir Private Endpoints, DNS e egress restrito antes de expor dados/serviços em produção. |
| Ingress Container Apps | O ambiente é interno, mas não há restrição exclusiva ao APIM. Se mantido sem Private Endpoint, implementar e validar as restrições de origem, DNS e testes positivos/negativos descritos na [topologia Azure](../architecture/azure-production-topology.md#container-apps-interno-sem-private-endpoint-requisitos-pré-produção). |
| Resiliência e operação | DLQ/replay, Disaster Recovery, multi-região, carga real e alertas operacionais completos não estão implementados ou validados. Decidir quais são requisitos de lançamento e implementar/testar os necessários. |
| Integração real | T33 permanece aberto; não há evidência de fluxo completo, RBAC efetivo, DNS ou runtime Azure. Executar os testes de aceite em ambiente controlado antes de liberar tráfego. |
| Pipeline | [deploy.yml](../../.github/workflows/deploy.yml) não executa `terraform apply`, não configura o backend remoto nem fornece as variáveis Terraform, e não tem uma etapa de atualização das Container Apps. O job de `plan` não gera artefato de plano para aprovação/aplicação. Não use esse workflow como pipeline produtivo sem completá-lo. |
| Bootstrap do ACR | A composição cria o ACR e as Container Apps no mesmo root, mas as imagens precisam existir antes de criar as apps. O procedimento abaixo usa um `-target` único e excepcional para o bootstrap do ACR; o ciclo deve ser eliminado em uma evolução da IaC. |
| Isolamento de negócio | Pela ADR-017, qualquer usuário autorizado pode acessar qualquer `accountId`. Formalizar a aceitação do risco; não há isolamento por comerciante. |

O detalhamento da topologia, das exposições e dos riscos está em
[topologia Azure](../architecture/azure-production-topology.md),
[evidências](../implementation/evidence-report.md) e
[análise de segurança](../architecture/security-architecture.md).

## 1. Aprovar requisitos e preparar o ambiente

Antes de criar recursos:

1. Obtenha uma assinatura Azure aprovada, responsável operacional, orçamento,
   limites de custo, região e quotas para Application Gateway WAF_v2, APIM,
   Container Apps, Function Flex Consumption, Cosmos DB, Service Bus Premium e
   ACR Premium. A região principal configurada é `brazilsouth`; a região de
   gerenciamento do Static Web App é `eastus2`.
2. Aprove os SLOs, retenção de dados/logs, estratégia de backup/restore, RTO/RPO,
   alertas, janela de mudança e procedimento de rollback. A estimativa de custos
   é apenas uma referência, não uma cotação nem um total validado.
3. Use uma estação administrativa ou runner controlado com Azure CLI, Terraform
   1.10.5, Git, Docker, JDK 25, Maven e Node.js 24. Confira as versões aprovadas
   pela organização antes do uso.
4. Execute e aprove a CI para o commit candidato: testes, cobertura, contratos
   OpenAPI, políticas APIM, Terraform e scans de dependências/imagens. Não
   publique artefatos enquanto houver falhas não aceitas formalmente.
5. Valide o suporte regional das SKUs, integração VNet, zonas e quotas. A
   validação estática atual não comprova que todas as opções estão disponíveis
   na assinatura/região escolhida.

## 2. Preparar Entra ID, DNS e certificado

1. Configure o tenant Microsoft Entra ID e os registros de aplicação da SPA e
   das APIs, usando Authorization Code + PKCE para a SPA. Crie os scopes de
   leitura/gravação definidos pelos contratos OpenAPI e pela ADR-017.
2. Alinhe issuer, URL do documento OpenID, audiences e scopes entre Entra ID,
   APIM, os dois serviços e frontend. Os serviços também validam JWT; não
   dependa apenas da política do APIM.
3. Reserve o FQDN público da API e controle o domínio da SPA. Configure o DNS
   para o IP público produzido pelo Application Gateway e valide a propriedade
   do domínio para o Static Web App.
4. Crie um Key Vault externo à composição atual e carregue o certificado PFX
   válido, com cadeia completa. Configure a rede para que o Application
   Gateway consiga acessar o vault e use, preferencialmente, URI do secret sem
   versão para permitir rotação controlada.
5. Informe ao Terraform o ID ARM do Key Vault e a URI do secret do certificado.
   O Terraform cria a identidade do Application Gateway e tenta atribuir
   `Key Vault Secrets User` ao escopo do vault; a identidade de deploy precisa
   ter autorização explícita para criar esse role assignment no vault externo.

## 3. Preparar estado remoto, identidade OIDC e RBAC de deploy

### Estado Terraform

Não há módulo de bootstrap para o backend no repositório. Crie a Storage
Account e o container de state por um processo de plataforma separado e
aprovado, antes de inicializar `prod`. Use uma conta dedicada, com acesso por
Entra ID, sem access keys, HTTPS/TLS, versionamento, soft delete, rede restrita
e auditoria. O lease de blob do backend AzureRM fornece o lock concorrente.
Proteja leitura, recuperação e retenção do state; ele pode conter valores
operacionais sensíveis.

A identidade que executa Terraform precisa de `Storage Blob Data Contributor`
no escopo da Storage Account/container do state. Não grave credenciais, state,
certificados, planos ou `*.tfvars` no Git.

### OIDC do GitHub Actions

Antes de usar os jobs Azure do workflow:

1. Crie uma identidade de workload dedicada e uma credencial federada no Entra
   ID com audience `api://AzureADTokenExchange` e subject
   `repo:danielcoruscant/cashflow-architecture-challenge:environment:prod`.
2. Proteja o environment GitHub `prod` com aprovação de responsáveis, branches
   permitidas e regras de mudança. Prefira identidades diferentes para
   provisionamento Terraform, publicação no ACR e deploy de aplicações; o
   workflow atual usa `AZURE_CLIENT_ID` compartilhado e precisará ser adaptado
   para essa separação.
3. Defina as variáveis de ambiente/repositório exigidas pelo workflow:
   `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`,
   `TERRAFORM_ROOT`, `ACR_NAME`, `ACR_LOGIN_SERVER`,
   `LANCAMENTOS_IMAGE_NAME`, `CONSOLIDADO_IMAGE_NAME`,
   `AZURE_FUNCTION_APP_NAME`, `AZURE_FUNCTION_RESOURCE_GROUP`,
   `AZURE_FUNCTION_REGION`, `VITE_ENTRA_CLIENT_ID`,
   `VITE_ENTRA_TENANT_ID`, `VITE_ENTRA_API_SCOPE` e `VITE_API_BASE_URL`.
   O valor esperado de `TERRAFORM_ROOT` é `infra/environments/prod`.
4. Configure o secret GitHub `AZURE_STATIC_WEB_APPS_API_TOKEN` para o job que
   publica a SPA. Não o substitua por um valor Terraform nem o exponha em logs.

### Permissões da identidade de implantação

Separe as permissões de gerenciamento das permissões de dados:

| Identidade/uso | Permissão necessária | Escopo |
|---|---|---|
| Terraform | Permissões de gerenciamento para criar/atualizar os recursos dos módulos e criar os role assignments declarados | Menor escopo viável, com atribuições temporárias/PIM e condições que limitem os papéis e principais permitidos. Não conceda `Owner` permanente. |
| Terraform | Autorização para criar o resource group com sufixo aleatório | A composição cria o resource group; a estratégia de bootstrap deve cobrir a permissão de criação em nível de assinatura sem deixar privilégio amplo permanente. |
| Terraform | Permissão para criar o role assignment da identidade do Application Gateway | Escopo do Key Vault externo informado ao Terraform. |
| CI de imagens | `AcrPush` | Somente no ACR de produção. |
| Deploy da Function | `Website Contributor` (ou role equivalente aprovado) | Somente no Function App de produção. |
| Backend Terraform | `Storage Blob Data Contributor` | Somente no backend do state. |

O primeiro `apply` merece uma decisão explícita de privilégio: o root cria um
resource group com nome aleatório e atribui papéis no Key Vault externo. Um
`Contributor` apenas em um resource group preexistente não cobre esse desenho.
Prefira refatorar o bootstrap para scopes controlados ou usar uma concessão
temporária, aprovada e auditada; revogue/reduza os privilégios após o bootstrap.
Não conceda `Owner` como atalho.

Um administrador autorizado pode atribuir as permissões de state, publicação e
deploy depois de obter os IDs ARM dos escopos e o **object ID** da identidade:

```bash
az role assignment create \
  --assignee-object-id "<DEPLOY_OBJECT_ID>" \
  --assignee-principal-type ServicePrincipal \
  --role "Storage Blob Data Contributor" \
  --scope "<STATE_CONTAINER_RESOURCE_ID>"

az role assignment create \
  --assignee-object-id "<RELEASE_OBJECT_ID>" \
  --assignee-principal-type ServicePrincipal \
  --role "AcrPush" \
  --scope "<ACR_RESOURCE_ID>"

az role assignment create \
  --assignee-object-id "<RELEASE_OBJECT_ID>" \
  --assignee-principal-type ServicePrincipal \
  --role "Website Contributor" \
  --scope "<FUNCTION_APP_RESOURCE_ID>"
```

Use IDs obtidos do tenant/assinatura corretos e revise o escopo antes de
executar. Não use client ID no campo `--assignee-object-id`. As atribuições de
runtime da tabela abaixo são feitas pela composição Terraform, não por estes
comandos.

## 4. Configurar variáveis Terraform com segurança

O root não contém valores de ambiente para estes campos obrigatórios. Forneça-os
por `TF_VAR_*` no environment protegido ou por arquivo fora do repositório, com
permissão de leitura restrita:

| Variável Terraform | Valor esperado |
|---|---|
| `publisher_name`, `publisher_email` | Responsável e contato operacional do APIM. |
| `entra_issuer`, `entra_openid_configuration_url` | Issuer e URL OIDC do tenant usado pelas APIs. |
| `lancamentos_audience`, `consolidado_audience` | Audiences registradas no Entra ID e nos Resource Servers. |
| `api_domain_name` | FQDN público do listener do Application Gateway. |
| `application_gateway_key_vault_id` | ID ARM do Key Vault externo. |
| `application_gateway_certificate_secret_id` | URI do secret que contém o PFX. |
| `lancamentos_image`, `consolidado_image` | URLs imutáveis das imagens no ACR, preferencialmente tag por SHA do commit. |
| `location`, `static_web_app_location`, `tags` | Revise os defaults conforme região e governança aprovadas. |

Em GitHub Actions, configure os `TF_VAR_*` no environment protegido; o
workflow atual ainda não os injeta. Se usar arquivo local, mantenha-o fora do
workspace ou confirme que `infra/.gitignore` o exclui e que não será copiado
para logs ou artefatos.

## 5. Validar e inicializar Terraform

Confirme o diretório e a identidade antes de cada operação:

```bash
export TF_ROOT=infra/environments/prod
terraform version
az account show --query '{subscription:id,tenant:tenantId}' --output table
terraform fmt -check -recursive infra
terraform -chdir="$TF_ROOT" init \
  -input=false \
  -lockfile=readonly \
  -reconfigure \
  -backend-config="resource_group_name=<STATE_RESOURCE_GROUP>" \
  -backend-config="storage_account_name=<STATE_STORAGE_ACCOUNT>" \
  -backend-config="container_name=<STATE_CONTAINER>" \
  -backend-config="key=cashflow/prod.tfstate" \
  -backend-config="use_azuread_auth=true"
terraform -chdir="$TF_ROOT" validate
```

Faça login previamente com a identidade autorizada. Em execução administrativa
local, use a autenticação Azure CLI aprovada pela plataforma. No GitHub Actions,
use federated OIDC: acrescente `-backend-config="use_oidc=true"`, configure
`ARM_CLIENT_ID`, `ARM_TENANT_ID`, `ARM_SUBSCRIPTION_ID`,
`ARM_USE_OIDC=true` e `ARM_USE_AZUREAD=true`, e conceda `id-token: write` ao job.
Substitua todos os placeholders do backend. Não use `-reconfigure` para
abandonar um backend/state existente; se houver state a migrar, pare e planeje
uma migração com backup e revisão.

## 6. Fazer bootstrap do ACR e publicar imagens

As Container Apps consomem imagens do ACR e o Terraform não constrói/publica
imagens. Como o root cria ACR e Container Apps juntos, faça este bootstrap
excepcional antes do primeiro apply completo:

1. Configure todas as variáveis Terraform requeridas. Para esta etapa, os valores
   de imagem ainda não serão usados, pois o alvo não inclui as Container Apps.
2. Gere, revise e aplique um plano limitado ao resource group e ao ACR:

   ```bash
   terraform -chdir="$TF_ROOT" plan \
     -input=false \
     -target=module.resource_group \
     -target=module.acr \
     -out=/caminho-seguro/acr-bootstrap.tfplan
   terraform -chdir="$TF_ROOT" show -no-color /caminho-seguro/acr-bootstrap.tfplan
   terraform -chdir="$TF_ROOT" apply -input=false /caminho-seguro/acr-bootstrap.tfplan
   ```

   `-target` é apenas uma exceção para quebrar o ciclo de dependência inicial,
   não o fluxo normal de atualização. O plano pode conter dados sensíveis:
   proteja o arquivo e remova-o após o uso conforme a política de retenção.
3. Identifique o nome e o login server do ACR criado:

   ```bash
   RG="$(terraform -chdir="$TF_ROOT" output -raw resource_group_name)"
   az acr list --resource-group "$RG" \
     --query '[].{name:name,loginServer:loginServer,id:id}' --output table
   ```

4. Atribua `AcrPush` à identidade que publica imagens no escopo apenas desse
   registry. O Terraform concede `AcrPull` às identidades das duas Container
   Apps, mas não concede `AcrPush` à identidade de CI.
5. Construa, faça scan e publique as duas imagens com tags imutáveis do commit.
   Registre os digests e use os endereços completos do ACR em
   `TF_VAR_lancamentos_image` e `TF_VAR_consolidado_image`. O job
   `build-images` de [deploy.yml](../../.github/workflows/deploy.yml) exige que
   ACR e variáveis já existam e atualmente pode rodar em paralelo ao `plan`;
   adapte a dependência/ordem antes de usar o workflow.

## 7. Planejar e aplicar a infraestrutura completa

Depois que as imagens existirem e as variáveis apontarem para seus digests:

```bash
terraform -chdir="$TF_ROOT" plan \
  -input=false \
  -out=/caminho-seguro/prod.tfplan
terraform -chdir="$TF_ROOT" show -no-color /caminho-seguro/prod.tfplan
```

Revise mudanças, destruições/substituições, custos, endereços de rede,
identidades, escopos RBAC, domínios, certificado, imagens e valores secretos
antes de aprovar. Aplique exatamente o plano aprovado, sem regenerá-lo:

```bash
terraform -chdir="$TF_ROOT" apply \
  -input=false \
  /caminho-seguro/prod.tfplan
```

Faça a mudança em janela aprovada e com responsável disponível para interromper
o rollout. Não execute `apply` se o plano mostrar remoção inesperada, mudança de
assinatura/região, exposição pública não aprovada ou ausência de role
assignments. O pipeline precisa ser atualizado para salvar o plano de forma
segura, exigir aprovação e aplicar o mesmo artefato; o workflow atual não faz
isso.

### RBAC de runtime criado pelo Terraform

Após o apply, confirme as atribuições nos escopos definidos pela composição:

| Identidade gerenciada | Papel | Escopo |
|---|---|---|
| Lançamentos | Cosmos DB Built-in Data Contributor | Database `fluxocaixa`. |
| Consolidado | Cosmos DB Built-in Data Contributor | Database `consolidado`. |
| Function Publisher | Cosmos DB Built-in Data Contributor | Database `fluxocaixa`, incluindo o Change Feed/leases. |
| Function Publisher | Azure Service Bus Data Sender | Tópico `lancamentos-topic`. |
| Consolidado | Azure Service Bus Data Receiver | Tópico `lancamentos-topic`. |
| Lançamentos e Consolidado | `AcrPull` | ACR. |
| Function Publisher | Storage Blob Data Owner, Storage Queue Data Contributor e Storage Table Data Contributor | Storage Account privado da Function. |
| Application Gateway | Key Vault Secrets User | Key Vault externo informado ao Terraform. |

Esses papéis são declarados no Terraform; não crie duplicatas manualmente.
Confira também que o deploy não introduziu chaves de conta, SAS ou credenciais
locais. O Emulator Cosmos não valida esses papéis nem substitui um teste de
identidade no Azure.

## 8. Publicar aplicações, configurar DNS e liberar tráfego

1. Confirme que as Container Apps estão saudáveis e executando os digests
   aprovados. As URLs/FQDNs das aplicações internas não devem ser publicadas
   diretamente.
2. Empacote e publique a Function CDC com o job de build/deploy ou procedimento
   equivalente aprovado. O pacote não é criado pelo Terraform. Verifique a
   identidade gerenciada, permissões de Storage/Cosmos/Service Bus, Change Feed
   leases e ausência de erros de inicialização.
3. Compile e publique a SPA com `VITE_ENTRA_CLIENT_ID`,
   `VITE_ENTRA_TENANT_ID`, `VITE_ENTRA_API_SCOPE` e
   `VITE_API_BASE_URL=https://<api-domain>/api/v1`. O token de publicação do
   Static Web Apps é um secret GitHub independente.
4. Configure DNS do domínio da API para o IP público de `api_public_ip`, valide
   TLS e a saúde do caminho Application Gateway → APIM → Container Apps. O
   módulo atual cria apenas o hostname padrão do Static Web App; configure e
   valide domínio customizado separadamente se necessário. Se o ambiente
   Container Apps permanecer sem Private Endpoint, aplique as restrições de
   ingress previstas e registre evidências de resolução DNS privada, acesso via
   APIM e bloqueio de acesso direto de origens não autorizadas, conforme a
   [topologia Azure](../architecture/azure-production-topology.md#container-apps-interno-sem-private-endpoint-requisitos-pré-produção).
5. Antes de liberar usuários, teste login e scopes Entra, respostas 401/403,
   operações de lançamento, consistência eventual do consolidado, duplicatas,
   correlação de logs, alertas, limites e comportamento de falha/recuperação.
   Compare com os SLOs aprovados; não declare metas atingidas sem medi-las.
6. Libere tráfego de forma gradual somente depois da aprovação operacional e
   segurança. Registre versão dos artefatos, digests, plano Terraform, aprovações,
   resultados de smoke tests e responsável pela mudança.

## 9. Verificação de RBAC e operação inicial

- Revise role assignments de control plane e identidades no Portal/CLI com
  escopo mínimo; confirme especialmente o backend, ACR, Function App e Key
  Vault. O principal do deploy não deve conservar permissões temporárias.
- Confirme saúde de Application Gateway, APIM, Container Apps, Function,
  Cosmos DB, Service Bus, Static Web Apps e Application Insights.
- Verifique logs/alertas e métricas de backlog, retries, DLQ, erros HTTP, falhas
  da Function, consumo/custo e disponibilidade. Configure alarmes antes de
  remover a supervisão da janela de mudança.
- Siga os procedimentos existentes em [runbooks operacionais](./runbooks.md).
  Os runbooks atuais não incluem replay/reenvio de DLQ nem reconstrução do
  Read Model; não faça essas operações manualmente.

## 10. Rollback e recuperação

1. Pare a promoção de tráfego e preserve logs, IDs de correlação, digests,
   plano aplicado e estado dos recursos.
2. Se a falha for apenas de aplicação, reimplante o digest anterior aprovado
   ou reverta a revisão da Container App/SPA pelo procedimento testado. Não
   altere manualmente o state Terraform.
3. Para mudança de infraestrutura, gere e revise um novo plano de reversão;
   não use `terraform destroy` como rollback. Mudanças de schema, dados,
   role assignments e políticas podem ser irreversíveis ou afetar outros
   componentes.
4. Restaure dados somente conforme política de backup aprovada e runbook
   exercitado. O backup periódico atual do Cosmos e a ausência de DR não
   comprovam recuperação dentro de um RTO/RPO.
5. Faça análise de causa, atualize o plano/runbooks e só retome a promoção após
   evidência de correção e aprovação.

Os comandos específicos para executar o plano de rollback não foram detalhados
neste documento. Defina-os e valide-os em um procedimento exercitado antes do
primeiro deploy em produção.

## Referências do repositório

- [Terraform `prod`](../../infra/environments/prod/)
- [README de infraestrutura](../../infra/README.md)
- [Topologia Azure de produção](../architecture/azure-production-topology.md)
- [Validação estática Terraform](../architecture/terraform-validation-status.md)
- [Estimativa de custos](../architecture/azure-cost-estimate.md)
- [Plano e roadmap de implementação](../implementation/implementation-plan.md)
- [Relatório de evidências](../implementation/evidence-report.md)
- [Backend AzureRM do Terraform](https://developer.hashicorp.com/terraform/language/backend/azurerm)
- [Exemplo Microsoft de Terraform com GitHub OIDC](https://learn.microsoft.com/en-us/samples/azure-samples/github-terraform-oidc-ci-cd/github-terraform-oidc-ci-cd/)
