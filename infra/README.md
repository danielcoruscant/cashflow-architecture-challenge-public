# Terraform da infraestrutura

O procedimento de implantação, incluindo bootstrap do backend, identidade
OIDC, RBAC, ordem dos `apply` e validação pós-deploy, está no
[guia de implantação produtiva](../docs/operations/production-deployment.md).
Ele descreve o processo planejado; não significa que houve `plan` autenticado
ou provisionamento Azure.

## Organização

- `modules/<recurso>/`: módulos reutilizáveis.
- `environments/prod/`: única composição Terraform mantida nesta entrega.
- `modules/apim/policies/cashflow.xml`: política aplicada à API agregada `api/v1`; importa os dois contratos e valida audience + escopo por rota. Os XMLs anteriores por serviço permanecem como referências, não são aplicados pela composição atual.

Não havia arquivos de ambiente não produtivo rastreados em Git; não foi removido nenhum diretório nem dado local não rastreado. A composição não faz deploy, login Azure, `plan` contra Azure ou `apply`.

A VNet de produção reserva uma subnet `/27` dedicada à integração da Function
Flex Consumption e outra subnet aos Private Endpoints. O Storage da Function
desabilita acesso público e usa endpoints privados para Blob, File, Queue e
Table, com zonas DNS privadas vinculadas à VNet. A subnet Flex não usa service
endpoints nem é compartilhada com Container Apps.

## Segredos e entradas

Não versionar `*.tfvars`, state, certificados, senhas, tokens, connection strings ou credenciais. O `infra/.gitignore` bloqueia arquivos `tfvars` e artefatos/state Terraform em módulos e ambiente; usar variáveis `TF_VAR_*` oriundas de secret store do pipeline para os dados operacionais do APIM. IDs OIDC, audiences, hostname da API, IDs do Key Vault e URI do certificado são parâmetros do ambiente.

Pré-requisitos de um futuro deployment:

- imagens Linux publicadas no ACR antes da criação das Container Apps;
- certificado PFX disponível como secret no Key Vault externo, com acesso de rede e RBAC para a identidade do Application Gateway;
- valores de issuer, OpenID configuration URL e audiences alinhados à configuração Entra ID;
- permissões de gerenciamento e atribuição RBAC da identidade de deploy.

Como o root `prod` cria o ACR e as Container Apps em conjunto, a publicação das
imagens requer o bootstrap do ACR antes do primeiro apply completo. A exceção
controlada com `-target` e seus riscos estão descritos no guia de implantação;
não use `-target` nas atualizações regulares.

## Estado remoto recomendado (não validado)

Configurar o backend `azurerm` externamente, em Storage Account dedicado e separado do state da aplicação. O Storage deve usar criptografia em repouso do serviço Azure (e CMK por Key Vault se a política exigir), HTTPS/TLS, acesso por Entra ID/RBAC sem access keys, versionamento, soft delete, restrição de rede/Private Endpoint e retenção protegida. O backend AzureRM usa lease de blob para lock concorrente do state. Bootstrap e recuperação do backend devem ter procedimento próprio.

O bloco de backend está vazio para impedir configuração de tenant/subscription/nomes em código. Usar `-backend-config`/variáveis protegidas em uma inicialização futura. **A criptografia e o lock são recomendações de desenho; nenhuma conta de state foi criada, configurada ou validada.** State pode conter dados operacionais sensíveis; controlar leitura, acesso, versões e logs de auditoria.

## Validação estática

Targets previstos: `modules/{resource-group,network,monitoring,cosmosdb,servicebus,acr,container-apps,function-app,apim,application-gateway,static-web-app}` e `environments/prod`.

Os resultados de `fmt`, `init`, `validate` e Trivy estão em `docs/architecture/terraform-validation-status.md`. Não executar `apply` como parte dessa fase.
