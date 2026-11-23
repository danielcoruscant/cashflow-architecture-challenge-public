# Security Architecture

## Objetivo

Definir as estratégias de segurança adotadas na solução de Controle de Fluxo de Caixa.

A arquitetura foi projetada considerando:

- Confidencialidade
- Integridade
- Disponibilidade
- Auditabilidade
- Menor privilégio
- Defesa em profundidade

---

# Princípios de Segurança

A solução adota os seguintes princípios:

- Zero Trust
- Least Privilege
- Defense in Depth
- Secure by Default
- Identity First

---

# Arquitetura de Segurança

```text
Usuário
    │
    ▼

Microsoft Entra ID

    │
    ▼

JWT

    │
    ▼

Application Gateway WAF

    │
    ▼

API Management

    │
    ▼

Serviços de Negócio

    │
    ▼

Managed Identity

    │
    ▼

Recursos Azure
```

---

# Gestão de Identidade

## Provedor de Identidade

Microsoft Entra ID

Responsabilidades:

- autenticação
- autorização
- emissão de tokens
- Single Sign-On

---

## Fluxo de Autenticação

Fluxo utilizado:

```text
OAuth2 Authorization Code Flow
+
PKCE
+
OpenID Connect
```

Aplicado para:

```text
React SPA
```

---

## Benefícios

- padrão de mercado
- compatível com Zero Trust
- sem armazenamento de senha na aplicação

---

# Autorização

## Token

Formato:

```text
JWT
```

Assinado pelo Entra ID.

---

## Validação

Responsáveis (defesa em profundidade):

```text
Azure API Management
Lançamentos Service e Consolidado Service (JWT Resource Server)
```

Validam:

- assinatura
- emissor
- expiração
- audiência

---

## Autorização por Operação

RBAC por escopo/papel do token (ADR-017): leitura de lançamentos, gravação de lançamentos e leitura do consolidado. Por decisão de negócio, todo usuário autorizado acessa qualquer conta (`accountId`); não há isolamento por comerciante. Esse risco é aceito e deve ser comunicado aos consumidores.

---

# Escopo de Hardening

Escopo desta entrega: JWT, Entra ID, RBAC por operação, Managed Identity, TLS 1.2+ e Application Gateway WAF v2. Hardening corporativo avançado permanece parcialmente implementado: o Storage da Function usa Private Link, mas Cosmos DB, Service Bus e ACR ainda permitem acesso por endpoints públicos. A rede privada completa para esses serviços não foi implementada na PoC devido ao tempo disponível para a entrega do desafio e à priorização do fluxo funcional e dos controles de identidade/autorização. O escopo e a recomendação para produção estão detalhados na [topologia Azure](./azure-production-topology.md).

## Private Endpoints e segmentação de rede

- **Implementado na configuração Terraform:** Private Endpoints e DNS privado para Blob, File, Queue e Table do Storage da Function, com acesso público desabilitado.
- **Pendente para produção:** Private Endpoints, DNS privado e desabilitação de acesso público para Cosmos DB, Service Bus e ACR, após testes de conectividade dos workloads e do pipeline de implantação.
- **Container Apps:** o ambiente usa load balancer interno e ingress dentro da VNet; não é um Private Endpoint do serviço, e a configuração atual não restringe o acesso exclusivamente ao APIM. Se mantido sem Private Endpoint em produção, restringir o ingress de cada app às origens autorizadas do APIM e testar acessos permitidos e bloqueados, conforme os [requisitos de rede e validação](./azure-production-topology.md#container-apps-interno-sem-private-endpoint-requisitos-pré-produção). APIM também tem VIP interno. O Application Gateway é o ponto de entrada público previsto para SPA e APIs; o Static Web Apps deve ser seu backend, sem acesso direto dos clientes. O listener/backend da SPA e o bloqueio de acesso direto à origem ainda não estão configurados no Terraform atual.
- **Azure Functions:** integração VNet de saída e Storage privado não significam endpoint de entrada privado. A Function Publisher é acionada por Change Feed e não expõe endpoint de negócio nesta composição; reavaliar o controle de entrada se triggers ou endpoints forem adicionados.

TLS, Managed Identity e RBAC são controles importantes, mas não substituem a restrição de rede. A recomendação de Private Endpoints para os serviços de dados e mensageria reduz a exposição e deve ser validada com DNS, rotas, regras de rede e acesso de implantação em ambiente Azure. A validação estática atual não comprova conectividade de runtime nem provisionamento.

---

# Segurança das APIs

## API Gateway

Azure API Management

Responsabilidades:

- JWT Validation
- Rate limiting de 120 chamadas por minuto por principal autenticado
- API Governance
- Versionamento
- Observabilidade

As políticas por API em `infra/modules/apim/policies/` repetem validação de issuer e audience para cada serviço e autorizam os escopos definidos no OpenAPI por método HTTP. Os named values de issuer, configuração OpenID e audiences são fornecidos pelo ambiente; nenhum valor sensível ou dependente de tenant é embutido nas políticas.
Após validar o JWT, ambas as políticas aplicam [`rate-limit-by-key`](https://learn.microsoft.com/en-us/azure/api-management/rate-limit-by-key-policy) pela claim `oid`, com fallback para `sub`; exceder o limite retorna HTTP 429. O limite é um baseline configurável e não foi calibrado por teste de carga.

Os endpoints `/actuator/health/liveness` e `/actuator/health/readiness` não são publicados pelas APIs do APIM. Eles são usados diretamente como probes das Container Apps; somente esses endpoints são liberados sem JWT nos serviços. A disponibilidade pública dos serviços deve permanecer desabilitada.

---

## Proteções

### Rate Limiting

As políticas APIM aplicam 120 chamadas por 60 segundos por principal autenticado. O limite reduz abuso e consumo excessivo, mas não representa um SLO nem foi validado sob carga.

---

### Header Validation

Não há política genérica de validação de headers configurada no APIM. Os serviços e o WAF fazem as validações sob sua responsabilidade; regras adicionais de headers permanecem evolução futura.

---

# Proteção Perimetral

## Web Application Firewall

Serviço:

```text
Azure Application Gateway WAF
```

---

## Objetivos

Proteção contra:

- OWASP Top 10
- SQL Injection
- XSS
- Path Traversal
- Request Smuggling

---

# Segurança entre Serviços

## Estratégia

Não serão utilizadas:

- senhas
- connection strings compartilhadas
- secrets em código

---

## Método

Utilização de:

```text
Managed Identity
```

---

# Recursos protegidos

## Cosmos DB

Acesso via:

```text
Managed Identity
+
RBAC
```

---

## Service Bus

Acesso via:

```text
Managed Identity
+
RBAC
```

---

## Azure Functions

Acesso via:

```text
Managed Identity
```

---

# Segurança dos Dados

## Dados em Trânsito

Todas as comunicações utilizam:

```text
TLS 1.2+
```

---

## Protocolos Utilizados

### Frontend

```text
HTTPS
```

---

### APIs

```text
HTTPS + JWT
```

---

### Service Bus

```text
AMQP 1.0 over TLS
```

---

### Cosmos DB

```text
HTTPS/TLS 1.2+
```

---

# Dados em Repouso

Proteção fornecida por:

```text
Azure Storage Encryption
```

Aplicável a:

- Cosmos DB
- Service Bus
- Logs
- Telemetria

---

# Segurança dos Eventos

## Event Tampering

### Risco

Manipulação indevida de eventos financeiros.

Exemplo:

```json
{
  "tipo": "CREDITO",
  "valor": 100
}
```

alterado para:

```json
{
  "tipo": "CREDITO",
  "valor": 1000000
}
```

---

## Mitigações

- TLS
- Managed Identity
- RBAC
- Immutable Ledger
- Validação do esquema do evento no consumidor

---

# Reenvio de Eventos (Replay de Mensagens)

## Risco

Reenvio ou redelivery de eventos já processados.

---

## Mitigações

- `eventId` (MessageId)
- Inbox Pattern (mecanismo principal; sem dependência da Duplicate Detection do Service Bus)
- TransactionalBatch

---

# Auditoria

## Estratégia

Immutable Ledger

Características:

- Append Only
- Source Of Truth
- Correção apenas por lançamento compensatório

---

## Benefícios

Permite:

- rastreabilidade
- reconstrução futura do consolidado (evolução, ADR-009)
- auditoria financeira

---

# Monitoramento de Segurança

## Ferramentas

### Azure Monitor

Coleta:

- métricas
- eventos
- telemetria

---

### Application Insights

Monitora:

- APIs
- dependências
- falhas

---

### Log Analytics Workspace

Centraliza:

- logs
- diagnósticos
- auditoria operacional

---

# Threat Model (STRIDE)

| Ameaça | Mitigação |
|----------|----------|
| Spoofing | Entra ID + JWT |
| Tampering | TLS + Managed Identity |
| Repudiation | Immutable Ledger |
| Information Disclosure | TLS + RBAC |
| Denial of Service | WAF + limite APIM por principal; proteção e capacidade não foram testadas sob carga |
| Elevation of Privilege | RBAC + Least Privilege |

---

# Gestão de Segredos

## Estratégia Atual

Managed Identity

---

## Evolução Futura

Caso necessário:

```text
Azure Key Vault
```

para armazenamento de:

- certificados
- segredos externos
- integrações com terceiros

---

# Conformidade com os Requisitos

A arquitetura atende aos requisitos de:

- autenticação
- autorização
- criptografia
- proteção de APIs
- segurança de integração
- observabilidade
- auditoria

mantendo aderência aos atributos de qualidade definidos para a solução. 【1-10c2ac】
