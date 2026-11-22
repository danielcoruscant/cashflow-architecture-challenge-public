# Validação estática Terraform — P6

**Data:** 21/11/2026. **Escopo:** 11 módulos em `infra/modules/*` e a composição
`infra/environments/prod`. Nenhum tenant ou recurso Azure foi acessado.

| Checagem | Resultado |
|---|---|
| `terraform fmt -check -recursive infra` | **Passou** com Terraform 1.10.5. |
| `terraform init -backend=false` | **Passou** nos 11 módulos e no root `prod`; não valida backend remoto. |
| `terraform validate` | **Passou** nos 11 módulos e no root `prod`, com AzureRM 4.81.0 e Random 3.9.1. |
| Trivy config 0.76.2, severidades HIGH/CRITICAL | **Passou** nos 11 módulos e no root `prod`; nenhum achado HIGH/CRITICAL. |
| Conftest / Azure policy library | **Não executado** — Conftest não estava instalado. |
| `plan` contra Azure, login ou `apply` | **Não executados**; nenhum recurso foi provisionado. |

O Trivy inicialmente identificou uma Storage Account permissiva para a Function
(AZU-0012). A configuração foi endurecida com acesso público desabilitado,
regras de rede com ação padrão `Deny` e Private Endpoints para Blob, File, Queue
e Table. A Function Flex Consumption usa uma subnet `/27` dedicada, delegada a
`Microsoft.App/environments`; a subnet dos Private Endpoints é separada e as
zonas DNS privadas são associadas à VNet. A subnet de integração não usa
service endpoints, conforme os requisitos do Flex Consumption.

Os resultados são validação estática; não comprovam conectividade nem
disponibilidade no Azure. Antes de implantação, confirmar no tenant que o
resource provider `Microsoft.App` está registrado, que a região/sku suporta as
opções configuradas e que a identidade de deploy tem permissões de rede e RBAC.
O uso de Private Endpoints e DNS precisa de validação de runtime após aprovação
e provisionamento controlado.

Na composição atual, Private Endpoints estão declarados apenas para Blob, File,
Queue e Table do Storage da Function. Cosmos DB, Service Bus e ACR mantêm acesso
público habilitado; recomenda-se restringi-los com Private Endpoints e DNS
privado antes de produção, após validar conectividade dos serviços e do pipeline
de implantação. Essa expansão de hardening de rede ficou fora da PoC devido ao
tempo disponível para a entrega do desafio. A análise completa, incluindo o
escopo de rede dos Container Apps e da Function, está na
[topologia Azure de produção](./azure-production-topology.md).

Referências:

- [Azure Functions networking options](https://learn.microsoft.com/en-us/azure/azure-functions/functions-networking-options)
- [How to use a secured storage account with Azure Functions](https://learn.microsoft.com/en-us/azure/azure-functions/configure-networking-how-to)
