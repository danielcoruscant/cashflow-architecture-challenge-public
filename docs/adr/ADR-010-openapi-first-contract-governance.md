# ADR-010 - OpenAPI First e Governança de Contratos

## Status

Aceito

---

# Contexto

A solução proposta é composta por múltiplos bounded contexts independentes:

- Lancamentos
- Consolidado

Cada contexto expõe capacidades específicas do domínio e evolui em ritmos diferentes.

O desafio enfatiza:

- documentação
- integração
- decomposição dos domínios
- capacidade de comunicação entre serviços
- justificativa das decisões arquiteturais

Além disso, o Azure API Management será utilizado como camada de publicação e governança das APIs.

Era necessário definir uma estratégia para:

- documentação das APIs
- evolução dos contratos
- integração entre consumidores e provedores
- governança dos endpoints expostos

---

# Problema

Em abordagens tradicionais Code First:

```text
Código
↓
Controller
↓
OpenAPI
```

o contrato passa a ser consequência da implementação.

Isso gera riscos de:

- divergência entre documentação e código
- contratos inconsistentes entre ambientes
- dificuldade de governança
- dependência entre times consumidores e provedores

---

# Decisão

A solução adotará:

```text
OpenAPI First
```

Também conhecido como:

```text
Contract First
```

O contrato OpenAPI será considerado a fonte oficial da API.

Toda implementação deverá ser derivada do contrato.

Fluxo adotado:

OpenAPI
↓
DTOs
↓
Controllers
↓
Use Cases
↓
Implementação

---

# Estrutura dos Contratos

Os contratos serão armazenados de forma independente dos microsserviços.

Estrutura:

```text
contracts
│
├── lancamentos-api
│   └── openapi.yaml
│
└── consolidado-api
    └── openapi.yaml
```

---

# Bounded Contexts

Cada bounded context será proprietário de seu próprio contrato.

## Lancamentos

Contrato:

```text
contracts/lancamentos-api/openapi.yaml
```

Responsável por:

```text
POST /lancamentos

GET /lancamentos

GET /lancamentos/{id}
```

---

## Consolidado

Contrato:

```text
contracts/consolidado-api/openapi.yaml
```

Responsável por:

```text
GET /consolidado

GET /consolidado/{data}
```

---

# Governança de Contratos

Cada contrato deve:

- possuir ciclo de vida próprio
- possuir versionamento próprio
- possuir documentação própria

Os contratos não devem compartilhar schemas de domínio.

Cada contexto é proprietário de seus próprios modelos.

---

# Azure API Management

O Azure API Management será responsável por publicar os contratos para consumidores externos.

Estrutura prevista:

```text
Cash Flow Platform

├── Lancamentos API
│
└── Consolidado API
```

Cada API será importada independentemente para o APIM.

---

# Estratégia de Versionamento

Cada contrato possuirá versionamento independente.

Exemplo:

```text
Lancamentos API

v1
v2
```

Sem impacto obrigatório em:

```text
Consolidado API
```

---

# Benefícios

## Baixo Acoplamento

Permite evolução independente dos microsserviços.

---

## Governança

Facilita gestão das APIs através do APIM.

---

## Documentação

Contratos tornam-se documentação executável.

---

## Integração

Consumidores conseguem integrar-se sem depender da implementação interna.

---

## Testes

Permite:

- Contract Tests
- Consumer Driven Contracts
- Validação automática dos endpoints

---

# Alternativas Avaliadas

## Code First

Fluxo:

```text
Implementação
↓
OpenAPI
```

### Vantagens

- simplicidade inicial

### Desvantagens

- baixa governança
- documentação dependente da implementação
- maior risco de inconsistência

### Decisão

Rejeitada.

---

## Shared Contract

Único contrato compartilhado para toda a plataforma.

### Vantagens

- simplicidade

### Desvantagens

- forte acoplamento
- evolução limitada
- conflitos de versionamento

### Decisão

Rejeitada.

---

# Consequências

## Positivas

- documentação padronizada
- maior governança
- integração simplificada
- evolução independente dos contextos
- alinhamento com DDD

---

## Negativas

- necessidade de manutenção dos contratos
- disciplina adicional durante o desenvolvimento

---

# Implicações para Desenvolvimento

Todos os microsserviços devem:

- implementar apenas endpoints definidos em contrato
- manter compatibilidade contratual
- atualizar o OpenAPI antes da implementação

Nenhuma API poderá ser criada diretamente na camada de controller sem atualização prévia do contrato.

---

# Implicações para Testes

A solução deverá suportar:

- Contract Tests
- OpenAPI Validation
- Consumer Compatibility Validation

---

# Relação com Outros ADRs

Relaciona-se com:

- ADR-001 - Microsserviços
- ADR-002 - CQRS
- ADR-005 - Azure Service Bus
- ADR-008 - Observabilidade

---

# Conclusão

A adoção de OpenAPI First garante que os contratos de integração sejam tratados como ativos arquiteturais de primeira classe.

Essa abordagem favorece:

- Governança
- Integração
- Evolução independente
- Documentação
- Testabilidade

e está alinhada com os princípios de DDD, microsserviços e arquitetura orientada a contratos adotados para a solução.