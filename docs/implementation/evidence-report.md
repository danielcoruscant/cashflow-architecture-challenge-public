# T33 — Relatório de evidências

**Status:** parcial; o gate final T33 permanece aberto.  
**Data:** 21/11/2026. Nenhum recurso Azure foi provisionado ou acessado.

Este relatório separa as verificações reproduzidas localmente, as validações
documentadas em etapas anteriores e os itens que continuam sem evidência. Ele
não representa prontidão produtiva nem substitui os critérios de T29–T32.

## Verificado nesta execução

Ambiente local: OpenJDK 25.0.5.1, Maven 3.9.12, JaCoCo 0.8.16, Node 24.21.0
e Vitest 5.1.1.

| Verificação | Comando / evidência | Resultado |
|---|---|---|
| Testes Java e cobertura por módulo | `bash tests/p9-local.sh`; [resultados P9](./p9-test-results.md) | 119 testes sem falhas ou skips. Gate Java de 80% de linhas e branches aprovado em cada módulo. |
| Cobertura Java sem código OpenAPI gerado | `python3 tests/p9-coverage.py` | Lançamentos 93,50%/94,05%; Consolidado 90,00%/93,10%; Publisher 91,30%/87,50%; Consumer 88,89%/90,00% (linhas/branches). |
| Build e testes frontend | `cd frontend && npm run build && npm run test:coverage` | Build passou; 39 testes. Linhas 98,24%, branches 91,12%, statements 97,50%, funções 94,44%; inclui `auth.ts` e `main.tsx`, excluindo somente código gerado e testes. |
| Pacote local Azure Function | Maven `clean package` e `verify` com nomes de validação locais | Empacotamento passou; não fez deploy nem acessou Azure. |
| Comandos Maven dos componentes | `clean verify` em Lançamentos, Consolidado e Consumer; `test` e empacotamento em Publisher | Todos passaram. Os testes usam clientes simulados ou construção local do SDK, sem conexão aos serviços Azure. |
| Políticas APIM | `python3 tests/validate-apim-policies.py` | XML bem formado; ambas as políticas limitam a 120 chamadas por 60 s por principal (`oid`, fallback `sub`) e preservam os escopos. O runtime APIM não foi testado. |
| Terraform estático | [status de validação P6](../architecture/terraform-validation-status.md) | Terraform 1.10.5, AzureRM 4.81.0 e Random 3.9.1; `fmt`, `init -backend=false` e `validate` passaram nos 11 módulos e no root `prod`. Trivy config 0.76.2 não encontrou HIGH/CRITICAL. |
| Comandos dos READMEs | [README do projeto](../../README.md), [frontend](../../frontend/README.md), READMEs dos serviços e integrações | Comandos Maven, build/cobertura frontend e empacotamento local da Function foram conferidos. Comandos que exigem credenciais ou runtimes Azure não foram executados. |

O comando Maven do Publisher requer identificadores para o plugin de
empacotamento. Foram usados valores temporários de validação; nenhum valor foi
gravado em arquivo de configuração ou usado para publicar recursos.

As imagens P7 foram reconstruídas com estas tags locais, sem publicação:

```sh
docker build --pull --file services/lancamentos-service/Dockerfile --tag cashflow-ci/lancamentos-service:validation-20261121 .
docker build --pull --file services/consolidado-service/Dockerfile --tag cashflow-ci/consolidado-service:validation-20261121 .
```

O scan usou `aquasec/trivy:0.76.2`, `--scanners vuln`,
`--severity CRITICAL,HIGH` e `--exit-code 0`; portanto os achados foram
avaliados na saída do relatório, não pelo código de saída do comando:

```sh
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v /tmp/cashflow-trivy-cache:/root/.cache aquasec/trivy:0.76.2 image --cache-dir /root/.cache/trivy --scanners vuln --severity CRITICAL,HIGH --exit-code 0 --format table cashflow-ci/lancamentos-service:validation-20261121
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v /tmp/cashflow-trivy-cache:/root/.cache aquasec/trivy:0.76.2 image --cache-dir /root/.cache/trivy --scanners vuln --severity CRITICAL,HIGH --exit-code 0 --format table cashflow-ci/consolidado-service:validation-20261121
```

## Não verificado ou incompleto

| Item do plano | Estado e limite |
|---|---|
| T29b — Cosmos Emulator/Testcontainers | **Executado localmente.** Testcontainers 1.20.6 iniciou a imagem oficial Linux vNext `EN20261019` (digest registrado em [resultados P9](./p9-test-results.md)); 1 teste de Lançamentos (persistência/leitura e partição) e 2 de Consolidado (batch/Inbox/idempotência e ETag obsoleto) passaram. O Emulator não aplica autenticação e cobre apenas um subconjunto da API NoSQL; os resultados não validam autenticação/RBAC nem o serviço Cosmos no Azure. Comandos e limitações em [resultados P9](./p9-test-results.md). |
| T29c — WireMock/MockServer e fluxo integrado | **Não executado.** As verificações atuais simulam os SDKs Java; não exercitam uma dependência HTTP WireMock/MockServer nem um fluxo integrado publisher–broker–consumer. WireMock não simula os protocolos AMQP do Service Bus ou Change Feed do Cosmos; os mocks atuais não substituem o aceite. O Service Bus Emulator permitiria testar AMQP localmente, mas requer aceitação de seus termos de licença e não foi iniciado. |
| T32a — Consolidado indisponível | Teste de contexto comprova que Lançamentos aceita débito sem Consolidado configurado; indisponibilidade distribuída real não foi simulada. |
| T32b — duplicata e ETag | **Validado localmente.** Cosmos Emulator confirmou batch atômico, Inbox/idempotência e resposta 412 para ETag obsoleto sem sobrescrever o saldo. O retry do repositório após conflito também é coberto por teste unitário simulado. |
| T32c — falha/commit/reconciliação | Falhas antes/depois do commit e reconciliação estão cobertas em simulações com fixture em memória; nenhuma queda real de banco ou broker foi injetada. |
| T32d — retomada do consumidor | Redelivery após nova instância foi simulada; não houve reinício real do processo nem validação de retomada do Service Bus. |
| T31 — estratégia de carga | **Aceite documental atendido.** [Estratégia de carga](../architecture/load-test-strategy.md) define ferramenta, cenários, perfil, métricas, fórmulas e metas. Carga, perda e P95 não foram medidos e não são declarados como resultados. |
| P7 — workflow completo e imagens | [CI](../../.github/workflows/ci.yml) e [deploy](../../.github/workflows/deploy.yml) tiveram YAML parseado localmente; SHAs e `actionlint` foram validados em etapa anterior. A CI agora valida também as políticas APIM. Não houve execução no GitHub. As imagens foram reconstruídas do worktree atual com `docker build --pull` e verificadas pelo Trivy 0.76.2 em 21/11/2026. Cada imagem apresentou **2 CRITICAL e 0 HIGH** no `app.jar`: CVE-2026-47884 e CVE-2026-47890 em `spring-webmvc 6.2.19`; o sistema operacional e o agente Application Insights não apresentaram achados HIGH/CRITICAL. O Trivy indica 7.0.9 como versão corrigida; a [assessoria oficial CVE-2026-47884](https://spring.io/security/cve-2026-47884/) lista 6.2.20 apenas em suporte Enterprise. Maven Central retornou 404 para `spring-webmvc 6.2.20` e Boot 3.5.18; não foi aplicado patch Enterprise sem repositório/licença. A busca nas fontes de serviços e integrações não encontrou `XsltView` nem endpoints SSE, o que reduz a aplicabilidade aparente, mas não elimina os achados do scan. O gate de segurança permanece aberto. |
| Telemetria Azure | Agente e configuração local foram conferidos, mas nenhum dado foi ingerido no Application Insights. |
| P1–P5 e P8 completos | Esta execução não revalidou individualmente todas as tarefas nem os serviços Azure reais; consulte os documentos arquiteturais e de operação referenciados no [plano](./implementation-plan.md). |

## Riscos residuais

- Sem tratamento de DLQ ou replay, mensagens esgotadas podem deixar o saldo
  divergente; falhas da Function após os retries também podem deixar eventos
  sem publicação. O ledger permanece a fonte planejada para uma reconstrução
  futura. As limitações e mitigações documentadas estão em
  [runbooks](../operations/runbooks.md) e no [plano](./implementation-plan.md).
- O tier APIM Developer não oferece SLA. A validação estática não comprova
  conectividade dos Private Endpoints, DNS, identidades, RBAC efetivo ou
  disponibilidade dos serviços Azure.
- As imagens atuais não têm achados HIGH no sistema operacional nem no agente,
  mas cada uma ainda contém dois achados CRITICAL em `spring-webmvc 6.2.19`
  (CVE-2026-47884 e CVE-2026-47890). O gate de segurança permanece aberto;
  não se deve declarar as imagens livres de vulnerabilidades.

## T35 — Auditoria de consistência (preliminar)

- Os links Markdown locais do repositório foram verificados; todos os destinos
  encontrados existem.
- Os comandos de build/testes documentados nos READMEs foram executados. Os
  comandos que exigem credenciais ou runtimes Azure permanecem explicitamente
  fora da validação local.
- A auditoria identificou e alinhou a política de rate limit APIM com o NFR
  (120 chamadas/60 s por principal) e alterou a descrição de correlação para
  não afirmar tracing distribuído via OpenTelemetry.
- A auditoria final não pode ser fechada enquanto T29c, os cenários integrados
  restantes de T32a/T32c/T32d e a resolução dos achados CRITICAL de imagem permanecerem
  pendentes. A CI/deploy também não foram executados no GitHub. Não há
  evidência de operação Azure; as metas de carga estão documentadas, mas não
  medidas.

## Decisão do gate

Os critérios documentais de T30, T31 e T29b foram atendidos localmente. T33
**não está concluído**: faltam T29c, T32a/T32c/T32d em ambiente de integração
apropriado e a resolução ou aceitação formal dos dois achados CRITICAL nas
imagens. Não declarar provisionamento, integração com serviços
Azure, desempenho ou operação produtiva como verificados.
