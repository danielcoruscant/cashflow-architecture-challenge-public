# Estimativa de custos Azure — produção

## Status e limites da consulta

Consulta realizada em **21/11/2026** (USD, região `brazilsouth`) à fonte **Azure Retail Prices API**, por meio da ferramenta Azure MCP de preços. As oito consultas abaixo retornaram `prices: []`; portanto, não há preços unitários verificáveis para compor um total mensal. Não foram consultados uma assinatura, o Azure Pricing Calculator autenticado nem um ambiente provisionado. **Não publique um total monetário ou use este documento como cotação/orçamento aprovado.**

| Filtro consultado | Resultado da consulta |
|---|---|
| Application Gateway / `WAF_v2` | Nenhuma linha retornada |
| API Management / `Developer` | Nenhuma linha retornada |
| Azure Container Apps / `Consumption` | Nenhuma linha retornada |
| Cosmos DB / `Serverless` | Nenhuma linha retornada |
| Service Bus / `Standard` | Nenhuma linha retornada |
| Storage / `Standard_LRS` | Nenhuma linha retornada |
| Azure Functions / `Y1` | Nenhuma linha retornada |
| Static Web Apps / `Standard` | Nenhuma linha retornada |

Fonte pública para nova cotação: [Azure Pricing Calculator](https://azure.microsoft.com/pricing/calculator/) e [Azure Retail Prices API](https://prices.azure.com/api/retail/prices). A resposta vazia pode refletir filtros de SKU/serviço, disponibilidade regional ou limites da ferramenta; não significa custo zero. O filtro de Functions `Y1` não é o SKU selecionado para Java 25; Flex Consumption `FC1` permanece sem cotação. Os filtros de Service Bus, ACR e Storage consultados também não cobrem todos os SKUs escolhidos abaixo.

## Escopo e premissas de produção

- Um único ambiente `prod`, conforme ADR-014; nenhum custo de dev, homologação, QA ou sandbox.
- Região primária `Brazil South`; Static Web Apps usa `East US 2` como localização do recurso de controle e distribui conteúdo globalmente.
- 730 horas/mês, 30 dias, carga inicial baixa, com alvo de até 50 req/s a validar por teste.
- Container Apps com duas réplicas mínimas por serviço, 0,5 vCPU/1 GiB por réplica e máximo de 10; não é cenário scale-to-zero.
- Application Insights e Log Analytics com retenção de 30 dias e limite inicial de ingestão de 1 GB/dia.
- Cosmos DB Serverless, uma região com redundância zonal e backup periódico.
- Sem desconto de reserva, benefícios de contrato, impostos, suporte, transferência de dados, DNS/domínio, Key Vault/certificado, build/publicação, ou custos de tráfego/egress.

## Perfil a estimar quando a consulta estiver disponível

| Componente de produção | SKU/configuração IaC | Principal unidade variável ou ressalva |
|---|---|---|
| Application Gateway | WAF_v2, zonas 1–3, autoscale 2–5 | Horas/instâncias, capacidade processada e regras WAF |
| API Management | Developer_1, VNet interno | Custo fixo; sem SLA, simplificação aceita na ADR-014 |
| Container Apps | Consumption, 2 apps, mínimo 2 réplicas/serviço | vCPU-segundos, GiB-segundos, requisições e réplicas mínimas |
| Cosmos DB | Serverless, zona redundante | RUs consumidas, armazenamento e backup |
| Service Bus | Premium, 1 messaging unit, zona redundante | Unidade Premium-hora e operações |
| Azure Functions | Flex Consumption `FC1`, Java 25 | Execuções, GB-s, instâncias prontas e armazenamento ZRS do host |
| ACR | Premium, zona redundante, admin desabilitado | Unidade de registry, armazenamento e transferência |
| Application Insights / Log Analytics | Workspace baseado, retenção 30 dias, limite de ingestão 1 GB/dia | GB ingeridos, retenção e exportação |
| Static Web Apps | Standard, distribuição global | Plano, tráfego e recursos contratados |
| Rede/identidades | VNet, subnets, IP público, Managed Identities | IP público e tráfego; egress/Private Link ainda não cotados |

## Método para completar a estimativa

Executar nova consulta com SKU ARM exato, região suportada e medidores apropriados; importar as dimensões do perfil acima no Pricing Calculator e registrar quantidade, horas, unidade de uso, preço/data e cenário de tráfego. Validar ingestão de telemetria, RU/s, réplicas mínimas, tráfego de saída e custos do Application Gateway/APIM. Manter a separação entre estimativa e cobrança real.

O preço precisa ser revisto antes de qualquer implantação. APIM Developer não oferece SLA e não satisfaz, sozinho, uma meta de disponibilidade produtiva; substituir por tier com SLA aumenta o custo. Também comparar Cosmos Serverless com throughput provisionado/autoscale conforme a carga medida e reduzir réplicas mínimas somente se os SLOs permitirem.
