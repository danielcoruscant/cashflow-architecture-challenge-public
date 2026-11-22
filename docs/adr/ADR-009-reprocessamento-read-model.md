# ADR-009 - Exclusão de Replay e Reconstrução Automática do Read Model

## Status

Aceito (replay documentado, não implementado)

---

# Contexto

A arquitetura proposta utiliza:

- DDD
- CQRS
- Event Driven Architecture
- Azure Service Bus
- Cosmos DB Change Feed
- Immutable Ledger
- CDC Publisher

O serviço de Lançamentos é considerado a fonte da verdade do domínio financeiro.

Os lançamentos são persistidos em um modelo:

- Append Only
- Immutable Ledger

O serviço de Consolidado mantém uma projeção derivada (Read Model) construída a partir dos eventos publicados pela plataforma.

O desafio possui tempo limitado para implementação e prioriza a capacidade de tomada de decisão arquitetural, documentação e justificativa das escolhas realizadas. 

---

# Problema

Podem ocorrer situações em que o Read Model precise ser reconstruído:

- correção de bugs na projeção
- alteração de regra de negócio
- inconsistência de dados
- recuperação de falhas
- auditoria

A estratégia atual baseada em Cosmos Change Feed foi projetada para processamento contínuo de eventos e não como mecanismo principal de replay histórico.

---

# Decisão

A solução adotará o Cosmos DB Ledger como sistema oficial de registro (System of Record).

O Read Model do Consolidado será tratado como:

- derivado
- descartável
- reconstruível

Em caso de necessidade de reconstrução:

1. O Read Model poderá ser descartado.
2. Os lançamentos serão relidos diretamente do Ledger.
3. Um processo de replay reconstruirá as projeções do Consolidado.

---

# Implementação Atual

**Replay, reprocessamento e reconstrução automatizada de projeções não são implementados** nesta entrega, conforme o [plano de implementação](../implementation/implementation-plan.md). O mecanismo é apenas documentado como evolução arquitetural.

Consequências operacionais desta exclusão:

- eventos perdidos pela Function após esgotar retries, ou mensagens enviadas à DLQ do sistema (ADR-015), não são republicados automaticamente;
- o saldo diário pode divergir do ledger nesses casos até que o replay exista;
- o ledger permanece íntegro e é a fonte para a futura reconstrução;
- a Inbox (ADR-012) deve ter retenção compatível com o replay futuro.

A arquitetura foi desenhada de forma a permitir a implementação futura sem alterações estruturais nos serviços existentes.

---

# Evolução Futura

Será criado um componente dedicado:

```text
Replay Service
```

Responsável por:

- leitura do Ledger
- reconstrução do Read Model
- reprocessamento parcial ou total

Fluxo previsto:

Cosmos Ledger
↓
Replay Service
↓
Consolidado Read Model

Restrição de fronteira: o Consolidado **não** deve ler diretamente o banco de Lançamentos. A reconstrução futura deve ocorrer por republicação a partir do contexto proprietário do ledger (Replay Service/Change Feed) ou por exportação controlada, preservando a Inbox para deduplicação.

---

# Alternativas Avaliadas

## Opção 1

Reprocessamento através do Azure Service Bus.

Fluxo:

Ledger
↓
Replay Publisher
↓
Service Bus
↓
Consumidores

### Vantagens

- reutilização da infraestrutura existente
- múltiplos consumidores podem ser reconstruídos simultaneamente

### Desvantagens

- maior custo operacional
- maior volume de mensagens
- maior tempo de reconstrução

---

## Opção 2

Reprocessamento direto do Ledger.

Fluxo:

Ledger
↓
Replay Service
↓
Read Model

### Vantagens

- simplicidade
- menor custo
- maior velocidade

### Desvantagens

- solução específica para o Consolidado

### Decisão

Selecionada como estratégia preferencial.

---

# Consequências

## Positivas

- preservação completa do histórico financeiro
- recuperação simplificada de falhas
- reconstrução integral das projeções
- aderência ao modelo de Immutable Ledger

---

## Negativas

- componente adicional deverá ser implementado futuramente
- aumento do tempo necessário para reconstruções completas em grandes volumes

---

# Justificativa Arquitetural

A adoção do Immutable Ledger garante que:

- nenhum lançamento seja perdido
- projeções possam ser descartadas sem impacto no domínio principal
- o sistema possua uma estratégia clara de recuperação

A implementação do Replay Service foi deliberadamente postergada para manter o foco na entrega dos requisitos prioritários do desafio dentro do tempo disponível, sem comprometer a evolução futura da arquitetura. 