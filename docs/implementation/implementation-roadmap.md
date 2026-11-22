# Roadmap de Implementação

## Objetivo e regra de precedência

Este roadmap organiza os marcos e o paralelismo do [plano de implementação](./implementation-plan.md). Os critérios de aceite e as dependências por tarefa definidos no plano são normativos; este roadmap não cria requisitos adicionais.

Um marco pode agrupar tarefas, mas cada tarefa só é concluída com seu próprio aceite e evidência. Testes locais, mocks e validações estáticas não comprovam execução ou funcionamento em Azure.

## Marcos

### M0 — Fundamentos e decisões

**Tarefas:** T01–T02, T03a–T03l, T04a–T04c.

**Saída verificável:** requisitos, contexto e ADRs alinhados; baseline Java 25 validado por módulo; configuração da Function sem valores fixos de ambiente.

As ADRs (T03a–T03k) são atualizadas/criadas e aceitas individualmente; só então T03l atualiza e valida o índice.

### M1 — Contratos e domínio

**Tarefas:** T05a–T05b, T06–T07, T36.

**Saída verificável:** contratos OpenAPI validados; regras de lançamento e projeção cobertas por testes unitários; clientes/interfaces gerados e consumidores compilando.

T06 e T07 podem avançar em paralelo a T05a/T05b. T36 só começa após os dois contratos.

### M2 — Ledger e publicação CDC

**Tarefas:** T08a–T08b, T09a–T09b, T10a–T10b, T11.

**Saída verificável:** configuração Cosmos testada por teste de contexto sem autenticação Azure; repositório coberto por cliente simulado; teste Cosmos Emulator identificado como executado ou não verificado; publisher com configuração e payload cobertos por testes; consumidor demonstrativo isolado e fora do pipeline de produção.

T11 é independente das tarefas de publicação e pode avançar assim que T01 estiver concluída.

### M3 — Consumo e projeção

**Tarefas:** T12a–T12b, T13–T15.

**Saída verificável:** processor de Service Bus separado do delegador de aplicação; settlement manual só após commit; validação e falhas do consumidor cobertas por testes; Inbox e projeção atômicas e idempotentes conforme os casos do plano; estratégias futuras documentadas sem implementação de replay/DLQ.

T14 pode avançar após as decisões de M0. T13 depende do domínio, repositório e settlement definidos no plano.

### M4 — Segurança e frontend

**Tarefas:** T16a–T16c, T17a–T17c.

**Saída verificável:** autenticação/autorização testada por serviço e política APIM revisada separadamente; cada fluxo principal do frontend testado com clientes simulados; cobertura medida em T30, não duplicada aqui.

Este marco pode avançar em paralelo com M2/M3 assim que seus contratos e regras de domínio diretos estiverem prontos.

### M5 — Terraform e topologia (paralelo)

**Tarefas:** T19a–T19k, T20, T21a–T21d, T22–T23.

**Saída verificável:** cada módulo validado isoladamente antes da composição `prod`; identidades/RBAC separados por recurso; topologia e estimativa documentadas; validação estática e análise de segurança registradas.

Não executar `apply` nem declarar backend remoto, permissões efetivas ou recursos Azure como verificados nesta entrega.

### M6 — CI/CD incremental (paralelo)

**Tarefas:** T24a–T24c, T25a–T25d, T26a–T26f.

**Saída verificável:** cada job de CI, varredura e etapa de deploy (OIDC, ACR, Function, SPA e plan/aprovação) tem aceite estático separado; o workflow completo passa no linter. Login OIDC, publicação e `apply` não são executados.

Os jobs podem ser adicionados conforme cada saída de build/teste esteja disponível, sem bloquear o desenvolvimento de outros módulos.
O procedimento operacional de produção, incluindo pré-requisitos, publicação,
validação e rollback, está no [guia de implantação produtiva](../operations/production-deployment.md).

### M7 — Observabilidade e operação

**Tarefas:** T27a–T27d, T28a–T28b.

**Saída verificável:** configuração e propagação de `correlationId` testadas localmente nos limites de componente; runbooks revisados com procedimentos e limites explícitos, sem replay ou tratamento de DLQ.

OpenTelemetry permanece evolução futura, não entrega deste marco.

### M8 — Testes e evidências

**Tarefas:** T29a–T29c, T30–T33.

**Saída verificável:** testes de contrato, integração e componente são reportados separadamente; cenários de resiliência têm resultados próprios; metas de carga são descritas como metas não medidas. T33 só pode ser fechado depois dos resultados de P6 (Terraform) e P7 (CI/CD e workflow de deploy), além dos testes T29a–T32d.

Executar T29a–T29c, T30 e T32a–T32d junto às respectivas implementações. T33 é o gate final de evidências, não substitui os testes individuais.

### M9 — Documentação e encerramento

**Tarefas:** T34a–T34d, T35.

**Saída verificável:** cada README documenta comandos conferidos para seu componente; T35 revisa consistência final entre o resultado, os documentos e as evidências, sem referências quebradas.

Manter documentação de cada componente junto à sua implementação; T35 é a verificação de encerramento.

## Paralelismo e caminho técnico

- M5 e a configuração inicial de M6 podem avançar desde M0, em paralelo com contratos e serviços.
- M1 permite desenvolver M2 e M4 por dependências específicas, sem esperar o fechamento da fase inteira.
- M3 depende dos componentes necessários de M1/M2; M7 instrumenta cada componente conforme fica disponível.
- M8 é contínuo. Somente a consolidação T33 aguarda os resultados requeridos.
- M9 também é contínuo para READMEs; somente a auditoria T35 é terminal.

O caminho técnico principal está detalhado pelas dependências de tarefa no plano, especialmente `T01 → T06 → T08a → T08b → T09a → T10b → T12a → T13 → T32a–T32d → T33 → T35`.

## Critérios de pronto

Os critérios são os da seção “Definição de pronto” do [plano de implementação](./implementation-plan.md). Em particular:

- builds, testes e cobertura precisam de evidência por módulo;
- Terraform e workflows são validados estaticamente;
- nenhum recurso Azure é provisionado nesta entrega;
- evidências distinguem itens verificados, não verificados e riscos residuais;
- não declarar SLA, carga em Azure ou operação produtiva como comprovados.

## Evoluções futuras fora do escopo

OpenTelemetry, Dapr, replay/reprocessamento, reconstrução automatizada, tratamento de DLQ, multi-região, Disaster Recovery, failover geográfico, autoscaling avançado, ambientes Dev/HML/QA/Sandbox, promoção entre ambientes, hardening corporativo avançado, testes de carga em Azure real, provisionamento real e operação produtiva completa.

### Evolução da plataforma Java

Planejar, como melhoria futura e em etapa própria, a migração dos serviços para
Spring Boot 4 com Spring Framework 7. Antes de alterar a baseline, confirmar a
compatibilidade com Java 25, Spring Security, Azure SDKs, dependências de
observabilidade, clientes gerados dos contratos OpenAPI e plugins Maven.
Executar a migração de forma coordenada por serviço, com testes de contrato,
integração, segurança e regressão, e atualizar a matriz de dependências e o
scan de vulnerabilidades. Esta evolução não altera a baseline atual nem faz
parte do escopo de implantação descrito neste roadmap.
