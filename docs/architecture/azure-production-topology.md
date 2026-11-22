# Topologia Azure — produção

## Fluxos

```mermaid
flowchart LR
  User[Browser / cliente] -->|HTTPS público: conteúdo estático| SWA[Static Web Apps Standard<br/>endpoint global]
  User -->|HTTPS 443 api.example.com| AGW[Application Gateway WAF_v2<br/>WAF Prevention / TLS]
  AGW -->|HTTPS para VIP privado| APIM[API Management Developer_1<br/>VNet internal]
  APIM -->|HTTPS via FQDN privado| ACA[Container Apps Environment<br/>internal / zonas]
  ACA --> LA[Lançamentos<br/>Managed Identity]
  ACA --> CO[Consolidado<br/>Managed Identity]
  LA -->|TLS + Cosmos data RBAC: fluxocaixa| DB[(Cosmos DB<br/>ledger / leases / projeção)]
  DB -->|Change Feed|   FN[Azure Function Flex Consumption<br/>Java 25 / Managed Identity]
  FN -->|TLS + Data Sender| SB[Service Bus Premium<br/>lancamentos-topic]
  FN -->|TLS + MI / Private Link| ST[Storage host<br/>acesso público desabilitado]
  SB -->|consolidado-sub / Data Receiver| CO
  CO -->|TLS + Cosmos data RBAC: consolidado| DB
  ACA -.->|telemetria| OBS[Application Insights / Log Analytics]
  FN -.->|telemetria| OBS
  AGW -.->|logs/metrics Azure Monitor| OBS
  Entra[Microsoft Entra ID<br/>issuer, audience, scopes] -->|JWT validado no APIM e serviço| APIM
```

O diagrama representa os recursos declarados em Terraform, não recursos provisionados nem conectividade testada. A VNet contém subnets dedicadas ao Container Apps, à integração da Function Flex Consumption e aos Private Endpoints; zonas DNS privadas para Blob, File, Queue e Table são vinculadas à VNet. Cosmos DB, Service Bus e ACR ainda usam endpoints públicos TLS com autenticação por Managed Identity; o Storage da Function usa Private Link e acesso público desabilitado.

## Exposição e fronteiras de confiança

| Componente | Exposição prevista | Justificativa / controle |
|---|---|---|
| Static Web Apps | Público, HTTPS e distribuição global | Entrega da SPA ao browser. O frontend chama a API por um domínio separado; o hostname padrão serve conteúdo estático, não APIs. |
| Application Gateway WAF_v2 | IP público, HTTPS 443 | Único ingresso público da API; WAF em Prevention, listener TLS com certificado obtido do Key Vault. |
| APIM | Somente endereço privado na VNet | VNet internal; aplica validação JWT, issuer/audience e autorização por escopo/operação. Tier Developer é uma simplificação sem SLA (ADR-014). |
| Container Apps | Ambiente com load balancer interno | Ingress dos apps é acessível pela VNet, inclusive pelo APIM; não há endpoint público externo do ambiente nem restrição exclusiva ao APIM configurada. |
| Function Publisher | Endpoint gerenciado de Function App, sem endpoint de negócio exposto | Trigger de Change Feed; integração VNet para acessar o Storage de host privado. Restringir acesso de entrada antes do deploy se triggers/recursos adicionais forem introduzidos. |
| Cosmos DB, Service Bus e ACR | Endpoints de serviço públicos habilitados | Tokens/RBAC, TLS e autenticação local desabilitada (quando suportado); o acesso à rede ainda não está limitado por Private Endpoint/firewall. |
| Storage da Function | Acesso público desabilitado; Private Endpoints para Blob, File, Queue e Table | Private DNS ligado à VNet; Managed Identity para acesso do host e deployment container. |

## Avaliação de Private Endpoints

O Terraform declara Private Endpoints somente para o Storage usado pela Function, nos subrecursos Blob, File, Queue e Table, com DNS privado associado à VNet. Cosmos DB, Service Bus e ACR mantêm `public_network_access_enabled = true` e não possuem Private Endpoints nesta composição. TLS, Managed Identity e RBAC protegem autenticação e autorização, mas não restringem esses serviços à rede privada.

Container Apps não usa um Private Endpoint próprio nesta topologia: o ambiente tem load balancer interno e os serviços são acessíveis dentro da VNet, inclusive pelo APIM interno. Não há regra de ingress que limite o acesso apenas ao APIM; outros recursos com conectividade de rede podem tentar acessar os serviços diretamente. A configuração de ingress das apps não deve ser interpretada isoladamente como exposição à Internet; é o ambiente interno que delimita a rede. A Function usa integração VNet para sua conectividade de saída e Storage privado; isso não equivale a um Private Endpoint de entrada para a Function, que não expõe endpoint de negócio nesta solução. Application Gateway e Static Web Apps permanecem públicos intencionalmente como pontos de entrada.

Para produção, recomenda-se implementar Private Endpoints para Cosmos DB, Service Bus e ACR, configurar e validar as zonas DNS privadas correspondentes e desabilitar o acesso público após confirmar a conectividade dos consumidores e do pipeline de implantação. O ACR também requer validar a resolução e conectividade dos endpoints de dados. A adoção deve ser compatível com as políticas de segurança e requisitos regulatórios do ambiente, não sendo uma afirmação de requisito legal universal.

Esses Private Endpoints não foram incluídos na PoC devido ao tempo disponível para a entrega do desafio e à priorização do fluxo funcional e dos controles de identidade/autorização. A composição Terraform foi validada estaticamente, mas nenhum recurso foi provisionado; conectividade, DNS e regras de rede ainda precisam de validação em Azure antes de qualquer uso produtivo.

### Container Apps interno sem Private Endpoint: requisitos pré-produção

Se a decisão for manter o ambiente interno sem Private Endpoint, o caminho pretendido é Application Gateway → VIP privado do APIM → FQDN privado da Container App. O isolamento da VNet não garante exclusividade ao APIM. Antes de liberar tráfego:

1. Manter o ambiente interno e confirmar que não há acesso público ao ingress. Configurar DNS privado e resolução dos FQDNs das apps pelo APIM para o IP interno do ambiente.
2. Restringir o ingress de **cada app** às origens legítimas do APIM, por exemplo com [restrições de IP no ingress](https://learn.microsoft.com/azure/container-apps/ip-restrictions) e/ou controles de rede compatíveis. Identificar e testar no Azure os IPs de origem realmente observados, inclusive após escala ou mudanças de topologia; uma regra baseada em subnet não identifica o APIM se outros recursos compartilharem essa origem. Negar as demais origens sem bloquear dependências da plataforma ou verificações de saúde.
3. Manter JWT e autorização nos serviços como defesa em profundidade; restrição por IP não autentica a identidade do APIM. Se for necessária prova de identidade exclusiva do chamador, definir e validar um mecanismo adicional de autenticação de serviço.
4. Validar o caminho completo em ambiente controlado: resolução DNS privada, TLS e chamadas autorizadas para as duas APIs via Application Gateway e APIM; confirmar também a saúde das apps e o funcionamento dos fluxos com suas dependências após aplicar as regras.
5. Testar acesso direto aos FQDNs de ambas as apps a partir de uma origem não autorizada com conectividade à VNet e confirmar o bloqueio **pela regra de rede**, inclusive usando credenciais válidas quando aplicável. Uma resposta 401/403 da aplicação, sozinha, não comprova o bloqueio de rede. Registrar evidências e plano de rollback antes da liberação.

Esse desenho pode evitar o custo adicional de Private Endpoint para o ambiente Container Apps, mas exige manutenção das regras, DNS e testes após mudanças de escala ou rede. A restrição e os testes acima são recomendações ainda não implementadas nem validadas; erros de configuração podem interromper o acesso às APIs.

O usuário autentica no Entra ID; o APIM e os serviços validam JWT independentemente, conforme ADR-017. IDs de tenant, issuer, audiences e domínio são entradas do ambiente, não credenciais gravadas no código.

Os dois contratos OpenAPI são mesclados em uma API APIM com sufixo único `/api/v1` (sufixos de APIs no mesmo APIM precisam ser únicos). A política seleciona o backend por rota e confere audience + escopo correspondente, preservando as rotas dos contratos sem publicar um backend diretamente.

## Identidades e permissões declaradas

| Identidade gerenciada | Escopo | Papel |
|---|---|---|
| Lançamentos ACA | Cosmos database `fluxocaixa` | Cosmos DB Built-in Data Contributor |
| Consolidado ACA | Cosmos database `consolidado` | Cosmos DB Built-in Data Contributor |
| Function Publisher | Cosmos database `fluxocaixa` | Cosmos DB Built-in Data Contributor (inclui checkpoint/lease do Change Feed) |
| Function Publisher | Tópico `lancamentos-topic` | Azure Service Bus Data Sender |
| Consolidado ACA | Tópico `lancamentos-topic` | Azure Service Bus Data Receiver |
| Serviços ACA | ACR | AcrPull |
| Function Publisher | Storage de host | Storage Blob Data Owner, Queue Data Contributor e Table Data Contributor |
| Application Gateway | Key Vault externo informado por ID | Key Vault Secrets User, limitado ao vault |

O papel de Receiver fica no tópico porque o escopo do tópico é suportado pelo RBAC do Service Bus; a subscription `consolidado-sub` é a única consumidora prevista no ADR-005. Data-plane RBAC do Cosmos é por database, como definido nos ADRs 007/011; não há chave de conta ou regra de autorização por comerciante.

## Disponibilidade, operação e limites

- Application Gateway WAF_v2 e IP Standard usam zonas 1–3 e autoscale configurado. Validar disponibilidade zonal na região antes do deploy.
- O ambiente de Container Apps habilita redundância zonal e mantém duas réplicas mínimas por API; valores são ponto de partida, não sizing validado.
- Cosmos DB Serverless usa região zonal única e backup periódico; não configura failover geográfico/DR.
- Service Bus Premium usa uma messaging unit e redundância zonal; `max_delivery_count=10`. DLQ e replay seguem fora do escopo (ADRs 009/015).
- A Function usa Flex Consumption (Java 25), integração VNet `/27` e Storage ZRS privado com identidade gerenciada. O host precisa do pacote implantado; Terraform não publica binário.
- O ACR é Premium, sem conta admin, com pull via identidade. Terraform não constrói nem publica imagens; as imagens devem existir antes de um futuro deploy.
- Application Insights/Log Analytics limitam ingestão e retenção como controle de custo. Alertas, dashboards e teste de carga em Azure não estão implementados/verificados.

## Riscos e próximos passos

1. **APIM Developer não tem SLA**: substituir por tier com SLA antes de aceitar SLO produtivo, considerando custo e modelo de rede.
2. **Endpoints públicos remanescentes**: antes de produção, planejar Private Endpoints, DNS privado e estratégia de egress para Cosmos, Service Bus e ACR; desabilitar o acesso público somente após validar conectividade dos workloads e do pipeline. O Storage da Function já está privado na configuração, mas sua conectividade ainda não foi exercitada em Azure.
3. **Function Consumption**: não existe allowlist de entrada/rede privada nesta composição; confirmar superfície do host/trigger e aplicar restrições suportadas pelo plano.
4. **Certificado**: o Key Vault é pré-requisito externo. A identidade do Application Gateway precisa de permissão de leitura e conectividade de rede ao vault; usar URI sem versão para rotação controlada.
5. **SLO, limites de escala, custo e suporte zonal**: confirmar com teste de carga, requisitos de negócio e Pricing Calculator. A estimativa atual não contém total precificado.
6. **Sem multi-região/DR**: recuperação geográfica requer decisão, runbooks e teste específicos.
