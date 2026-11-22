# Plano de Implementação

> Este documento define o escopo, as tarefas, as dependências e os critérios de aceite desta entrega. O roadmap organiza sua execução; não amplia nem contradiz este plano.


# 1. Objetivo e escopo

Completar a solução de fluxo de caixa (lançamentos e saldo diário consolidado por conta) demonstrando arquitetura, padrões de integração, modelagem de domínio e decisões arquiteturais, com Terraform, CI/CD e documentação.

## 1.1 Escopo definido para implementação nesta entrega

| Área | Escopo |
|---|---|
| Domínio | Ledger append-only; correção somente por lançamento compensatório; múltiplas contas (`accountId`); saldo por conta e data de negócio; BRL; `America/Sao_Paulo` |
| Integração | Cosmos Change Feed → Azure Function (publisher) → Service Bus → consumidor do Consolidado → Inbox → projeção diária |
| Segurança | JWT Resource Server, Microsoft Entra ID, RBAC por operação, Managed Identity, TLS 1.2+, Application Gateway WAF v2 |
| Plataforma | Cosmos DB, Service Bus, Azure Functions, API Management, Application Gateway WAF |
| Observabilidade | Application Insights, logs estruturados, `correlationId` ponta a ponta |
| Entrega | Terraform e GitHub Actions para o ambiente único `prod` |
| Stack | Java 25, Spring Boot 3.x, Maven, React + TypeScript |

## 1.2 Documentado, não implementado

OpenTelemetry, Dapr, replay, reprocessamento e reconstrução automatizada, tratamento de DLQ, multi-região, Disaster Recovery, failover geográfico, autoscaling avançado, ambientes Dev/HML/QA/Sandbox, promoção entre ambientes, hardening corporativo avançado, testes de carga em Azure real, provisionamento real e operação produtiva.

## 1.3 Decisões descartadas

Outbox Pattern (substituído por CDC + Inbox) e dependência de Duplicate Detection do Service Bus para consistência de negócio.

## 1.4 Premissas de execução

- Nenhum recurso Azure é provisionado ou alterado nesta entrega; Terraform é apenas validado estaticamente (`fmt`, `validate`, análise de segurança). `plan` e `apply` exigem acesso e aprovação futuros.
- Testes usam JUnit 5 e Mockito; integração usa Testcontainers, Cosmos Emulator (quando suportado pelo ambiente), WireMock e MockServer. Azure Service Bus, Azure Functions e Azure Change Feed são validados localmente por testes unitários, de componente, mocks e contratos, sem reproduzir seus runtimes/serviços reais. O relatório de evidências (T33) separa o que foi verificado do que não foi.
- Execução em Azure permanece fora do escopo; o projeto não deve alegar provisionamento ou comportamento real de Azure como verificado.
- Os arquivos em `target/`, `coverage/` e relatórios anteriores **não** são evidência atual; todo resultado deve ser reexecutado.

---

# 2. Padrões e decisões de desenho que as tarefas devem respeitar

## 2.1 Nomenclatura (padronizada nesta revisão)

| Item | Valor |
|---|---|
| Identificador de conta | `accountId` (substitui `contaId`) |
| Cosmos account | única por ambiente |
| Database do Lançamentos | `fluxocaixa` (containers `lancamentos` e `leases`) |
| Database do Consolidado | `consolidado` (container `consolidado-diario`) |
| Partition key | `/accountId` em `lancamentos` e `consolidado-diario`; `/id` em `leases` |
| Service Bus | tópico `lancamentos-topic`, subscription `consolidado-sub` |
| Evento de integração | `LancamentoCriado` (único tipo; não há atualização nem exclusão) |

Dois databases preservam a regra "nenhum serviço acessa o banco de outro contexto" com RBAC de dados por database.

## 2.2 Ledger e compensação

- Um lançamento é um documento imutável: `id`, `accountId`, `tipo` (`CREDITO`/`DEBITO`), `valor` (BRL, decimal positivo, 2 casas), `data` (data de negócio), `correlationId`, `causationId`, `compensaLancamentoId` (opcional).
- Correção = novo lançamento de tipo oposto, mesmo valor, referenciando o original. Não há `PUT`/`PATCH`/`DELETE` nas APIs.
- Cada documento gera exatamente um evento; `eventId` = `id` do documento.
- `correlationId` e `causationId` são gravados no documento no momento da escrita (a Function só os propaga).

## 2.3 Projeção idempotente (Inbox)

- A Inbox e o saldo diário ficam no **mesmo container e na mesma partição lógica** (`accountId`), pois `TransactionalBatch` do Cosmos só opera em uma partição.
- Saldo diário: documento `id = {accountId}:{yyyy-MM-dd}` com `creditos`, `debitos`, `saldo`.
- Processamento: `TransactionalBatch` = *create* do documento de Inbox (`id = eventId`) + *replace* do saldo com `If-Match` (ETag). Conflito `409` na Inbox = duplicata (confirma a mensagem sem alterar o saldo); falha de ETag = nova leitura e retry limitado.
- A projeção é aditiva (comutativa), portanto a ordem de chegada não afeta o resultado; o Service Bus não usa sessões.
- A mensagem só é concluída (`complete`) após o commit do batch; sem auto-complete.

## 2.4 Limites aceitos pelo escopo

Como DLQ e replay não são implementados, as consequências devem estar documentadas (T14, T33); T28a/T28b cobrem a identificação operacional de falhas:

1. Após esgotar `maxDeliveryCount`, o Service Bus move a mensagem para a DLQ do sistema; sem processo de tratamento, o saldo permanece divergente até haver replay futuro.
2. Se a Function esgotar os retries do lote, o Change Feed avança e eventos podem deixar de ser publicados; sem replay, a divergência não é corrigida automaticamente.
3. O ledger permanece íntegro e é a fonte para reconstrução futura.
4. Mitigação dentro do escopo: logs, métricas e alertas documentados; runbooks de reinício e validação.

---

# 3. Documentação revisada nesta etapa

| Documento | Ajuste |
|---|---|
| `docs/implementation/implementation-plan.md` | Escopo, tarefas, dependências e critérios de aceite desta entrega |
| `docs/implementation/implementation-roadmap.md` | Marcos e sequência alinhados a este plano |
| `docs/domain/context-map.md` | Ledger append-only, `accountId`, CDC + Inbox, sem DLQ/atualizar/excluir |
| `docs/adr/` | ADR-003/004/005/009/0011 atualizadas; ADR-012 a 017 criadas; índice criado |
| `docs/architecture/*` | NFRs, atributos de qualidade, riscos, segurança e custos alinhados |
| `docs/diagram/*.drawio` | Rótulos de Duplicate Detection ajustados |
| `README-PROJETO.md` | Escopo, ambiente único, fluxo final e estrutura alinhados |
| `.github/instructions/…`, `integrations/cdc-consumer/README.md` | Outbox removido; consumidor demonstrativo isolado |

---

# 4. Estado atual do código (base para as tarefas)

| Componente | Situação |
|---|---|
| Contratos OpenAPI | Sem `accountId`, sem paginação, sem segurança; respostas `501` placeholder |
| `lancamentos-service` | Domínio e casos de uso existem; `CosmosLancamentoRepository` e `CosmosConfiguration` vazios; `permitAll` |
| `consolidado-service` | Consulta e mapeamentos existem; `CosmosConsolidadoRepository`, `CosmosMensagemProcessadaRepository`, `AtualizarConsolidadoProjection`, `ServiceBusConfiguration` e consumidor não implementados; `permitAll` |
| `cdc-function` | Publica documentos no tópico; `createLeaseContainerIfNotExists = true` contradiz o README; `pom.xml` fixa `rg-lab`/`centralus`; usa `System.getenv` e `DefaultAzureCredential` |
| `cdc-consumer` | Apenas registra mensagens na mesma subscription do Consolidado (consumo concorrente) |
| Frontend | Dashboard, MSAL, clientes gerados; sem `accountId` nem compensação |
| `infra/`, `.github/workflows/` | Vazios |
| Java | Os quatro projetos já declaram Java 25; Azure Functions suporta Java 25 em GA (verificar no plano do runtime escolhido) |

---

# 5. Etapas e tarefas

Convenção: cada tarefa lista **arquivos-alvo**, **critério de aceite** e **dependências por ID**. Uma tarefa só está concluída quando seu próprio aceite é reproduzível; fases são agrupamentos de trabalho, não transações indivisíveis. Testes que dependem de serviços Azure reais ficam explicitamente fora do aceite local e não podem ser reportados como verificados. `[P]` indica tarefa paralelizável.

## P1. Revisão arquitetural

| Tarefa | Descrição | Aceite |
|---|---|---|
| **T01** | Congelar requisitos funcionais e não funcionais em `docs/architecture/non-functional-requirements.md` com a matriz da seção 7 | NFRs sem referência a Outbox, DLQ, replay ou ambientes múltiplos como entregáveis |
| **T02** | Atualizar `context-map.md`, `docs/diagram/*`, fluxos, segurança | Fluxo único: API → Ledger → Change Feed → Function → Service Bus → Consumidor → Inbox → Projeção |
| **T03a** | Atualizar ADR-003 | Decisão, contexto e consequências revisados individualmente e alinhados a este plano |
| **T03b** | Atualizar ADR-004 | Decisão, contexto e consequências revisados individualmente e alinhados a este plano |
| **T03c** | Atualizar ADR-005 | Decisão, contexto e consequências revisados individualmente e alinhados a este plano |
| **T03d** | Atualizar ADR-009 | Decisão, contexto e consequências revisados individualmente e alinhados a este plano |
| **T03e** | Atualizar ADR-011 | Decisão, contexto e consequências revisados individualmente e alinhados a este plano |
| **T03f** | Criar ADR-012 | Decisão, contexto e consequências revisados individualmente e alinhados a este plano |
| **T03g** | Criar ADR-013 | Decisão, contexto e consequências revisados individualmente e alinhados a este plano |
| **T03h** | Criar ADR-014 | Decisão, contexto e consequências revisados individualmente e alinhados a este plano |
| **T03i** | Criar ADR-015 | Decisão, contexto e consequências revisados individualmente e alinhados a este plano |
| **T03j** | Criar ADR-016 | Decisão, contexto e consequências revisados individualmente e alinhados a este plano |
| **T03k** | Criar ADR-017 | Decisão, contexto e consequências revisados individualmente e alinhados a este plano |
| **T03l** | Atualizar `docs/adr/README.md` após T03a–T03k | Índice lista ADR-001 a ADR-017 sem duplicatas; todos os links locais existem |
| **T04a** | Confirmar Java 25 nos quatro POMs e propriedades de build | `mvn -v` e build individual de cada módulo Java com JDK 25 |
| **T04b** | Remover `rg-lab`/`centralus` fixos da configuração da Function e documentar configuração do runtime | Busca não encontra esses valores fixos; teste/verificação de configuração confirma leitura de valores externos |
| **T04c** | Alinhar imagens e instruções de desenvolvimento à versão Java suportada | Cada imagem/instrução declara a versão exigida; validação por build de imagem ou inspeção automatizada de configuração |

Dependências: T01 não tem pré-requisitos; T02 depende de T01; T03a–T03k dependem de T01/T02; T03l depende de T03a–T03k; T04a–T04c dependem de T01.

## P2. Contratos e regras de domínio

| Tarefa | Arquivos-alvo | Aceite |
|---|---|---|
| **T05a** | `contracts/lancamentos-api/openapi.yaml` | `accountId` obrigatório na escrita e consulta; paginação; `bearerAuth` e escopos por operação; remoção dos `501`; contrato validado |
| **T05b** | `contracts/consolidado-api/openapi.yaml` | `GET /consolidado/{data}` recebe `accountId`; `bearerAuth` e escopos por operação; `404` para saldo inexistente; remoção dos `501`; contrato validado |
| **T06** | `services/lancamentos-service/.../domain/**`, `application/**` | Valor > 0 com 2 casas; data válida; compensação referencia lançamento existente e inverte o tipo; sem alteração/exclusão; testes unitários de casos negativos |
| **T07** | `services/consolidado-service/.../domain/**`, `application/**` | Saldo = créditos − débitos por `accountId` + data; separação de contas e dias; testes de arredondamento e compensação |
| **T36** | Interfaces Java e `frontend/src/generated/**` | Clientes/interfaces gerados a partir de T05a/T05b; diff reproduzível e compilação dos consumidores |

Dependências: T05a/T05b, T06 e T07 dependem de T01. T36 depende de T05a/T05b.

## P3. Persistência, CDC e publicação

| Tarefa | Arquivos-alvo | Aceite |
|---|---|---|
| **T08a** | `CosmosConfiguration`, `application.properties` | Configuração lê endpoint/database externos e usa `DefaultAzureCredential`, sem chave estática; teste de contexto/configuração verifica nomes de propriedades, credencial selecionada e criação dos clientes sem autenticar em Azure |
| **T08b** | `CosmosLancamentoRepository`, `LancamentoDocument` | Partition key `/accountId`; consultas por conta/data paginadas; erros mapeados sem `UnsupportedOperationException`; testes com cliente Cosmos simulado verificam operações, filtros, paginação e mapeamento de erro |
| **T09a** | Repositório e casos de uso do Lançamentos | Persiste somente o documento do ledger; não cria outbox; testes unitários do repositório/caso de uso passam sem Cosmos real |
| **T09b** | Testes de integração do Lançamentos | Teste Testcontainers + Cosmos Emulator executado quando suportado; se indisponível, registrar explicitamente como não verificado, sem bloquear T09a nem declarar integração aprovada |
| **T10a** | `integrations/cdc-publisher/function/cdc-function/**` | `createLeaseContainerIfNotExists = false`; leases pré-provisionados; endpoint e identidade configuráveis; verificação de configuração sem credenciais ou recursos Azure reais |
| **T10b** | Mapeamento de documento para evento do publisher | Evento contém `eventId`, `correlationId`, `causationId`; teste unitário verifica payload serializado e `eventId` idêntico ao documento; retry do host é documentado como limite não executado localmente |
| **T11** | `integrations/cdc-consumer/**` | Preservado como exemplo isolado; não usa `consolidado-sub`, contém aviso no README e é excluído do pipeline de produção |

Dependências: T08a depende de T01; T08b depende de T06/T08a; T09a depende de T06/T08b; T09b depende de T09a; T10a/T10b dependem de T05a e T09a; T11 depende de T01.

## P4. Consumo e projeções

| Tarefa | Arquivos-alvo | Aceite |
|---|---|---|
| **T12a** | `ServiceBusConfiguration` e adaptador/processor de Service Bus; `LancamentoCreatedConsumer` continua como delegador da aplicação | Processor com `autoComplete` desativado e lock renovado; confirmação (`complete`) somente depois do retorno bem-sucedido da projeção; teste com processor/contexto simulado verifica commit → complete e erro → sem complete, sem Service Bus real |
| **T12b** | Validação e política de falhas do adaptador/`LancamentoCreatedConsumer` | Evento inválido é rejeitado conforme política definida; falha transitória resulta em retry/abandon; testes unitários verificam cada cenário; sem implementar tratamento de DLQ |
| **T13** | `CosmosMensagemProcessadaRepository`, `CosmosConsolidadoRepository`, `AtualizarConsolidadoProjection` e contratos de repositório necessários | Substituir os `UnsupportedOperationException`; aplicar Inbox + projeção num `TransactionalBatch` da mesma partição (`accountId`); testes verificam duplicata, concorrência/ETag, falha antes/depois do commit e mensagens fora de ordem |
| **T14** | `docs/adr/ADR-009…`, `docs/operations/` | Documentar estratégia futura de replay, rebuild, reprocessamento e republicação; revisão documental confirma que não há implementação nesta entrega |
| **T15** | Configuração Resilience4j nos clientes síncronos elegíveis | Enumerar os clientes realmente presentes; cada timeout/retry/circuit breaker configurado tem teste próprio. Se não houver cliente elegível, registrar N/A; não criar chamadas síncronas entre serviços |

Dependências: T12a/T12b dependem de T05a/T05b e T10b; T13 depende de T07, T09a e T12a; T14 depende de T03d; T15 depende dos clientes implementados em T08b/T12a.

## P5. Segurança e frontend

| Tarefa | Arquivos-alvo | Aceite |
|---|---|---|
| **T16a** | `SecurityConfiguration` do Lançamentos | JWT valida assinatura, emissor, audiência e expiração; RBAC diferencia leitura/gravação; testes `401`/`403` |
| **T16b** | `SecurityConfiguration` do Consolidado | JWT valida assinatura, emissor, audiência e expiração; RBAC de leitura; testes `401`/`403` |
| **T16c** | Política APIM e probes | Políticas JWT/escopos correspondem aos contratos; `/actuator/health` é acessível somente às probes; validação estática da configuração |
| **T17a** | `frontend/src/**` | Seleção de conta/data e consulta de saldo/histórico; testes do fluxo usando clientes simulados |
| **T17b** | `frontend/src/**` | Criação e compensação de lançamento; teste verifica submissão e resposta do cliente gerado |
| **T17c** | `frontend/src/**` | Estados de carregamento, erro, vazio e consistência eventual sinalizada; testes dos estados; cobertura é aferida somente em T30 |

Dependências: T16a depende de T05a; T16b depende de T05b; T16c depende de T05a/T05b e T16a/T16b; T17a depende de T36 e T07; T17b depende de T36 e T06; T17c depende de T17a/T17b.

## P6. Terraform

Estrutura oficial: `infra/modules/<recurso>/` e apenas `infra/environments/prod/`. Dev, HML, QA e Sandbox ficam documentados para evolução futura; remover `infra/environments/dev`.

| Tarefa | Aceite |
|---|---|
| **T19a** | Módulo Resource Group criado e validado isoladamente |
| **T19b** | Módulos Log Analytics e Application Insights criados e validados isoladamente |
| **T19c** | Módulo Cosmos DB (databases, containers e leases) validado isoladamente |
| **T19d** | Módulo Service Bus (tópico, subscription e `max_delivery_count` explícito) validado isoladamente |
| **T19e** | Módulo ACR validado isoladamente |
| **T19f** | Módulo Container Apps (+ environment) validado isoladamente |
| **T19g** | Módulo Function App (+ plano e Storage de host) validado isoladamente |
| **T19h** | Módulo APIM validado isoladamente |
| **T19i** | Módulo Application Gateway WAF v2 validado isoladamente |
| **T19j** | Módulo Static Web App validado isoladamente |
| **T19k** | Compor módulos no ambiente `infra/environments/prod/`; manter apenas esse ambiente e remover `infra/environments/dev/` |
| **T20** | Topologia documentada em `docs/architecture/`: Internet → WAF → (SWA / APIM) → Container Apps; justificar exposições públicas |
| **T21a** | Identidade gerenciada atribuída a cada componente que a requer; validar configuração e ausência de chaves de aplicação estáticas |
| **T21b** | Cosmos data-plane RBAC por database, com permissões mínimas; validar principal e escopo das atribuições |
| **T21c** | Service Bus RBAC: sender na Function e receiver no Consolidado; validar principal, papel e escopo |
| **T21d** | Documentar estado remoto criptografado e lock; garantir que nenhum `.tfvars` sensível esteja versionado; não alegar validação do backend remoto |
| **T22** | Executar `terraform fmt -check`, `init -backend=false`, `validate` e análise de segurança por módulo e no root `prod`; registrar resultado de cada alvo. Isso não valida provisionamento nem backend remoto |
| **T23** | `docs/architecture/azure-cost-estimate.md` somente para produção, com região, SKUs, premissas, fonte e data da consulta |

Dependências: T19a–T19j são paralelizáveis após T01; T19k depende de T19a–T19j; T20 depende de T19k; T21a depende de T19f/T19g; T21b depende de T19c; T21c depende de T19d; T21d depende de T19k; T22 depende de T19a–T19k e T21a–T21c; T23 depende de T01.

## P7. GitHub Actions

| Tarefa | Entrega | Aceite |
|---|---|---|
| **T24a** | Job/matriz Maven para os quatro módulos Java em JDK 25 | Build e testes passam por módulo; cada resultado é identificado individualmente |
| **T24b** | Job frontend | `npm ci`, build, testes e relatório de cobertura passam; artefatos identificáveis |
| **T24c** | Job de validação OpenAPI | Ambos os contratos são validados e o job falha diante de contrato inválido |
| **T25a** | Job de validação Terraform | Verificações estáticas previstas em T22 passam e reportam cada módulo/root |
| **T25b** | Job de varredura de segredos | Varredura executa separadamente e reporta achados sem expor valores |
| **T25c** | Job de varredura de dependências e imagens | Varreduras executam e publicam resultados separados |
| **T25d** | Hardening dos workflows | Permissões mínimas e versões fixadas das actions são verificadas estaticamente |
| **T26a** | Job OIDC em `.github/workflows/deploy.yml` | Permissões `id-token` mínimas e configuração de login declaradas; validação estática passa. Login real não é executado |
| **T26b** | Job de build/publicação de imagens no ACR | Build, tag, destino e push estão configurados; referências de imagem conferidas. Push real não é executado |
| **T26c** | Job de deploy da Function | Artefato, destino e ordem de deploy estão configurados e referenciados corretamente. Deploy real não é executado |
| **T26d** | Job de publicação da SPA | Build, artefato e destino estão configurados e referenciados corretamente. Publicação real não é executada |
| **T26e** | Job de `terraform plan` e gate de aprovação | Ambiente `prod`, saída do plan e aprovação antes de `apply` estão conectados. `plan` contra Azure e `apply` não são executados |
| **T26f** | Validação estática do workflow completo | `actionlint` passa em `.github/workflows/deploy.yml`; não comprova OIDC nem deploy real |

Dependências: T24a–T24c e T25b–T25d podem iniciar após T01; T24a exige T04a, T24b exige T17a–T17c, T24c exige T05a/T05b; T25a exige T22; T26a depende de T01; T26b depende de T19e/T24a; T26c depende de T19g/T21a/T24a; T26d depende de T19j/T24b; T26e depende de T19k/T21d/T22; T26f depende de T26a–T26e.

O procedimento operacional para preparar e executar uma implantação produtiva,
incluindo validações e rollback, está no [guia de implantação produtiva](../operations/production-deployment.md).

## P8. Observabilidade

| Tarefa | Aceite |
|---|---|
| **T27a** | Configurar Application Insights e logs estruturados no Lançamentos; captura local de telemetria verifica configuração e campos de log, sem alegar ingestão confirmada em Azure |
| **T27b** | Configurar Application Insights e logs estruturados no Consolidado; captura local de telemetria verifica configuração e campos de log, sem alegar ingestão confirmada em Azure |
| **T27c** | Configurar Application Insights e propagação de `correlationId` na Function Publisher; teste unitário verifica os metadados publicados |
| **T27d** | Verificar propagação `correlationId` entre API, ledger, evento, Service Bus e Consolidado por testes de componente/contrato locais; registrar limites de rastreamento sem runtime Azure |
| **T28a** | `docs/operations/runbooks.md`: procedimento de reinício da Function e verificação de leases/lag; checklist revisável, sem instruções de replay |
| **T28b** | `docs/operations/runbooks.md`: procedimento de reinício do consumidor e inspeção de falhas, backlog, mensagens pendentes, tentativas e logs; checklist revisável, sem tratamento de DLQ |

Dependências: T27a depende do serviço Lançamentos estar disponível; T27b depende do serviço Consolidado estar disponível; T27c depende de T10a/T10b; T27d depende de T12a/T13; T28a depende de T10a/T19c/T21a; T28b depende de T12a/T13 e T21c. OpenTelemetry continua fora do escopo de implementação.

## P9. Testes

| Tarefa | Aceite |
|---|---|
| **T29a** | Testes de contrato/API cobrem registrar crédito/débito, compensar, consultar por conta/data e permissão negada; execução local sem Azure |
| **T29b** | Testes de integração Cosmos via Testcontainers/Emulator executam quando suportados; se não, reportar explicitamente como não verificados, sem sucesso substituto |
| **T29c** | Testes de componente com WireMock/MockServer cobrem publisher/consumidor e dependências simuladas; nenhum resultado é descrito como teste de runtime Azure |
| **T30** | Cobertura ≥ 80% (linhas e branches), exclusões justificadas, por módulo |
| **T31** | `docs/architecture/load-test-strategy.md` define cenário, ferramenta, perfil de carga e fórmulas para 50 req/s, perda ≤ 5% e P95 < 200 ms; esses valores são metas, não resultados medidos nesta entrega |
| **T32a** | Teste com Consolidado indisponível comprova que lançamentos continuam aceitos no ambiente de teste |
| **T32b** | Testes comprovam que duplicata não altera saldo e que conflito de ETag repete a leitura sem perder atualização |
| **T32c** | Testes cobrem falha antes/depois do commit e reconciliação ledger × saldo |
| **T32d** | Teste de reinício do consumidor comprova retomada compatível com Inbox e ausência de dupla aplicação |
| **T33** | `docs/implementation/evidence-report.md` consolida resultados, comandos e versões em “verificado”, “não verificado” e “riscos residuais”, incluindo limites da seção 2.4; revisão confirma que cada afirmação aponta para uma evidência produzida |

Dependências: T29a depende de T05a/T05b/T06/T07/T16a/T16b; T29b depende de T08b/T13; T29c depende de T10b/T12a/T12b; T30 depende das suítes do módulo correspondente; T31 depende de T01; T32a depende de T05a/T06/T09a; T32b/T32c dependem de T13; T32d depende de T12a/T13; T33 aguarda a conclusão e evidências de P1–P8, incluindo T19a–T19k, T20, T21a–T21d, T22–T23, T24a–T24c, T25a–T25d, T26a–T26f e T27a–T32d.

## P10. Documentação final

| Tarefa | Aceite |
|---|---|
| **T34a** | `README-PROJETO.md`: pré-requisitos, escopo real/verificado, execução local, build, testes e endpoints; comandos conferidos com os scripts existentes |
| **T34b** | `frontend/README.md`: pré-requisitos, configuração, execução, build e testes do frontend; comandos conferidos |
| **T34c** | READMEs de `services/*`: configuração, execução, build e testes por serviço; cada README é validado separadamente |
| **T34d** | READMEs de `integrations/*`: configuração, execução, build e testes por integração; cada README é validado separadamente |
| **T35** | Auditoria final de consistência entre arquitetura, ADRs, Terraform, CI/CD, escopo, evidências e roadmap; lista de verificações sem referências quebradas |

Dependências: T34a–T34d são atualizados junto às respectivas tarefas e conferidos após elas; T35 é o gate final após todas as tarefas de P1–P9 e T34a–T34d.

---

# 6. Ordem de execução e caminho crítico

As dependências são as declaradas por ID em cada fase; não há bloqueio automático entre fases inteiras. Trabalho de infraestrutura (P6), CI inicial (P7), segurança/frontend (P5) e documentação operacional pode avançar em paralelo quando seus pré-requisitos diretos estiverem prontos.

Fluxo técnico principal: `T01 → T06 → T08a → T08b → T09a → T10b → T12a → T13 → T32a–T32d → T33 → T35`, com os ramos `T05a/T05b → T36 → T17a/T17b`, `T05a/T05b → T12a`, `T07 → T13`, e P6/P7 executados incrementalmente em paralelo. T33 e T35 são gates finais de evidências e consistência, não tarefas de implementação independentes.

T29a–T32d devem ser executados junto às respectivas entregas; não se deve esperar a conclusão de P8 para começar os testes de unidade, contrato e componente.

---

# 7. Matriz de requisitos × evidência

| Requisito | Etapas | Evidência esperada |
|---|---|---|
| Serviço de lançamentos e consolidado diário | P2–P5 | T05a/T05b, T06–T07, T08a/T08b, T12a/T13 e T29a |
| Consolidado indisponível não derruba lançamentos | P3–P4, P9 | T32a executado em ambiente de teste |
| 50 req/s, perda ≤ 5% | P9 | T31 documenta cenário e metas; não constitui comprovação de desempenho |
| Mapeamento de domínios e desenho de arquitetura | P1 | Context map, diagramas, ADRs |
| Decisões justificadas | P1 | ADR-001 a ADR-017 |
| Testes | P9 | T29a–T32d; JUnit 5, Mockito, Testcontainers, Cosmos Emulator quando suportado, WireMock, MockServer, Vitest e JaCoCo; sem serviços Azure reais |
| README e execução local | P10 | T34a–T34d |
| Estimativa de custos, observabilidade, segurança (diferenciais) | P5, P6, P8 | T16a–T16c, T23, T27a–T28b |
| IaC e CI/CD | P6–P7 | T19a–T26f: módulos e jobs validados separadamente; OIDC/deploy apenas configuracionais |

---

# 8. Riscos e questões abertas

| Risco | Tratamento |
|---|---|
| Divergência de saldo sem replay/DLQ (seção 2.4) | Aceito e documentado; alertas e runbooks; ledger preservado |
| Contenção de ETag em contas muito ativas | Retry limitado; observar métricas de conflito; evolução: particionamento por conta+data |
| Hot partition por `accountId` | Documentado em ADR-011; limite de 20 GB por partição lógica |
| Perda de lote da Function após esgotar retries | Alerta de falhas; documentação do limite; replay como evolução |
| APIM Developer não oferece SLA | Registrado no custo e na ADR-014; SKU de produção é evolução |
| Terraform não aplicado | `plan`/`apply` pendentes; nenhuma alegação de infraestrutura funcional |
| Acesso global entre contas (decisão de negócio) | Registrado na ADR-017; RBAC por operação, sem isolamento por comerciante |

---

# 9. Definição de pronto desta entrega

- Código dos fluxos P2–P5 compila com Java 25 e passa nos testes; cobertura ≥ 80%.
- Contratos, ADRs, diagramas, NFRs, riscos e READMEs consistentes com o escopo e as decisões deste plano.
- Terraform e workflows validados estaticamente; nenhum recurso Azure criado.
- Relatório de evidências publicado com limites e itens não verificados explícitos.
- Nenhuma afirmação de prontidão produtiva, SLA atingido ou carga validada em Azure.

---

# 10. Escopo definido para implementação

O escopo definido para implementação nesta entrega é:

- Java 25 e Spring Boot 3.x
- Ledger append-only e lançamentos compensatórios
- Cosmos DB e Cosmos Change Feed
- Azure Function Publisher e Azure Service Bus
- Inbox Pattern
- JWT e Microsoft Entra ID
- Managed Identity
- API Management e Application Gateway WAF
- Application Insights, logs estruturados e `correlationId`
- Terraform e GitHub Actions
- Ambiente único `prod`

Esta lista descreve o escopo do plano; não substitui evidências de implementação ou execução, que são registradas em T33.

# 11. Escopo documentado, não implementado

- OpenTelemetry
- Dapr
- Replay e reprocessamento
- Reconstrução automatizada de projeções
- Tratamento de DLQ
- Multi-região
- Disaster Recovery e failover geográfico
- Autoscaling avançado
- Hardening corporativo avançado
- Ambientes Dev, HML, QA e Sandbox
- Promoção entre ambientes
- Testes em Azure real
- Provisionamento real
- Operação produtiva completa

# 12. Fluxo arquitetural final

```text
Internet
    │
    ▼
Application Gateway WAF
    │
    ▼
API Management
    │
    ▼
Lançamentos API
    │
    ▼
Cosmos Ledger (append-only)
    │
    ▼
Cosmos Change Feed
    │
    ▼
Azure Function Publisher
    │
    ▼
Azure Service Bus Topic
    │
    ▼
Consolidado Consumer
    │
    ▼
Inbox Pattern
    │
    ▼
Projeção de saldo diário
```

# 13. Resumo executivo

## Escopo definido para implementação

Java 25; Spring Boot 3.x; APIM; Application Gateway WAF; Cosmos DB; CDC; Azure Function; Azure Service Bus; Inbox Pattern; JWT; Microsoft Entra ID; Managed Identity; Application Insights; logs estruturados; `correlationId`; Terraform; GitHub Actions; ambiente único `prod`.

## Apenas documentado

OpenTelemetry; Dapr; replay; reprocessamento; reconstrução automática; DLQ; multi-região; Disaster Recovery; failover; autoscaling avançado; ambientes Dev/HML/QA/Sandbox; promoção entre ambientes; testes Azure reais; provisionamento real; operação produtiva completa.
