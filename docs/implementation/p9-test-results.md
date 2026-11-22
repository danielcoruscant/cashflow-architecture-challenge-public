# P9 — resultados locais parciais

Registro de 2026-11-21, não é o relatório final T33. P1–P8 não foram
revalidados integralmente; esta retomada complementou a evidência de P7 e T29b.
Nenhum serviço Cosmos no Azure foi acessado.

## Verificado

Ambiente: OpenJDK 25.0.5.1, Maven 3.9.12, JaCoCo 0.8.16, Python 3.14.4,
Node 24.21.0 e Vitest 5.1.1. Execução Java:

```sh
sh tests/p9-local.sh
```

O script executa `mvn -o -q -f <pom> clean test jacoco:report` nos quatro
módulos e depois `python3 tests/p9-coverage.py`. Foram aprovados 42 testes de
Lançamentos, 48 de Consolidado, 16 do publisher e 13 do consumer demonstrativo
(119 no total), sem falhas ou testes ignorados. O comando completo retornou
**0** e o gate de cobertura foi aprovado.
Resultados reproduzíveis em `target/surefire-reports/` e
`target/site/jacoco/jacoco.xml` de cada módulo após a execução.

| Módulo Java | Linhas integral | Branches integral | Linhas sem gerados | Branches sem gerados |
|---|---:|---:|---:|---:|
| Lançamentos | 63,73% | 53,21% | 93,50% | 94,05% |
| Consolidado | 76,67% | 73,97% | 90,00% | 93,10% |
| Publisher | 91,30% | 87,50% | 91,30% | 87,50% |
| Consumer demonstrativo | 88,89% | 90,00% | 88,89% | 90,00% |

O relatório integral é o JaCoCo original, sem exclusões configuradas. A
segunda métrica exclui somente arquivos comprovadamente presentes em
`target/generated-sources/openapi/src/main/java` e ausentes de `src/main/java`;
o script lista os arquivos. Entry points, configurações e todo o restante do código
de produção permanecem no denominador. O gate Java exige 80% de linhas e
branches na segunda métrica para **cada** módulo e foi aprovado nos quatro.
A diferença entre as métricas integral
e sem código gerado decorre somente das classes OpenAPI geradas listadas acima.

## Integração Cosmos via Testcontainers (T09b/T29b)

Os testes abaixo foram executados separadamente com sucesso (1 teste em
Lançamentos e 2 em Consolidado, sem falhas ou skips), usando Testcontainers
1.20.6, Docker 29.10.0, Java 25 e a imagem oficial
`mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator` (release
`EN20261019`, digest
`sha256:09dcd04e7149975f0427e6cb09781681f407fc74d01e54fc9d6410c36795cda3`):

```sh
mvn -o -ntp -f services/lancamentos-service/pom.xml -Dtest=CosmosEmulatorIT test
mvn -o -ntp -f services/consolidado-service/pom.xml -Dtest=CosmosEmulatorIT test
```

O teste de Lançamentos persistiu e consultou um lançamento no container real do
Emulator e confirmou isolamento por chave de partição. Os testes de Consolidado
aplicaram um evento por batch, repetiram o mesmo `eventId` e confirmaram que a
segunda aplicação não alterou o saldo e que o registro da Inbox foi persistido;
também confirmaram que uma substituição com ETag obsoleto é rejeitada com 412
sem sobrescrever o valor atualizado. O retry do repositório após conflito segue
coberto por teste unitário com SDK simulado.

O Emulator vNext oferece somente a API NoSQL em gateway mode, não aplica
autenticação e tem suporte parcial a recursos do Cosmos DB. Portanto, esses
resultados validam a interação dos repositórios e do batch com o Emulator, não
autenticação/RBAC nem conectividade ou semântica integral do serviço Azure. O
teste não usa nem valida Azure Service Bus.

Medição frontend incluindo todo o código de produção escrito:

```sh
cd frontend
npm run build
npm run test:coverage
```

Build e 39 testes (9 arquivos) passaram. Cobertura: linhas 98,24% (112/114),
branches 91,12% (113/124), statements 97,50% (117/120) e funções 94,44%
(34/36). O gate exige pelo menos 80% em cada indicador. A configuração exclui
somente SDKs gerados e arquivos de teste; `src/services/auth.ts` e `src/main.tsx`
estão incluídos e ambos atingiram 100% nos quatro indicadores. Os quatro novos
testes de `authService.test.ts` cobrem configuração ausente, normalização da
configuração, conta ativa e fallback, ausência de contas, obtenção do token e
configuração dos clientes OpenAPI.

Casos e evidências locais:

| Caso | Evidência | Limite |
|---|---|---|
| Crédito, débito, compensação, autenticação e autorização; consulta por conta/data | `LancamentosApiContractTest`, `ConsolidadoApiContractTest` | MockMvc, repositórios e clientes simulados; não há validação integral de schema OpenAPI em runtime |
| Débito aceito sem Consolidado no contexto | `postDebitoDevePersistirNoLedgerSemServicoDeConsolidadoNoContexto` | Mostra independência local; não simula queda de um processo ou serviço remoto |
| Duplicata e conflito de ETag, releitura e retry limitado | `CosmosConsolidadoRepositoryTest` | Respostas e conflitos do SDK simulados; não verifica o comportamento real de ETag no Cosmos |
| Falha antes do batch e falha do ACK após commit; duplicata após nova instância; saldo do cenário 100 − 25 = 75 | `LocalRedeliveryIntegrationTest` | Partição/atomicidade em memória simuladas; ledger é fixture; não reinicia processo, banco ou broker |
| Publicação, lote parcial, falha, payload/timestamp, configuração e health check | `LancamentoCdcFunctionTest`, `LancamentoEventTest`, `ServiceBusPublisherConfigurationTest`, `HealthCheckFunctionTest` | Sender real do SDK apenas construído e fechado; não há conexão nem publicação em Azure |
| Configuração e ciclo de vida do consumer demonstrativo, falha de inicialização, interrupção e handlers | `LancamentoConsumerTest`, `LancamentoConsumerRuntimeTest` | Processor simulado nos fluxos; construção do SDK local, sem conectar ao Service Bus |
| Estados de autenticação, login e erros de API; inicialização da aplicação | `AuthGate.test.tsx`, `main.test.tsx`, `apiError.test.ts` | MSAL e root React simulados; nenhuma autenticação externa |

## Não verificado

- T29b: os dois testes Testcontainers/Emulator descritos acima foram executados.
  Autenticação/RBAC e compatibilidade integral com o Cosmos DB no Azure não são
  validados pelo Emulator vNext.
- T29c: não foi executado teste com WireMock/MockServer nem um fluxo integrado
  entre publisher e consumidor. Os testes existentes simulam as dependências
  Java; nenhum runtime Azure foi testado.
- T32a: o teste de contexto comprova que Lançamentos aceita débito sem um
  serviço Consolidado configurado; uma queda distribuída real não foi simulada.
- T32b: duplicidade, atomicidade e rejeição de ETag obsoleto foram validadas
  no Cosmos Emulator; a retentativa do repositório após conflito está coberta
  por teste unitário simulado.
- T32c: falhas antes/depois do commit e reconciliação seguem simuladas com
  fixture em memória; não houve partição/queda real de banco ou broker.
- T32d: redelivery com nova instância foi simulado; reinício real do processo
  consumidor e retomada do Service Bus não foram verificados.
- T30 Java atingiu o limiar local por módulo.
- T31: o aceite documental está atendido por
  `docs/architecture/load-test-strategy.md`, que define ferramenta, cenários,
  perfil, fórmulas e metas. A carga real não foi executada; throughput, perda
  e P95 permanecem sem medição e não são declarados como resultados.
- T33: [relatório parcial](./evidence-report.md) produzido; o gate final
  permanece aberto até fechar ou registrar os bloqueios de T29c, T32 parcial
  e vulnerabilidades das imagens.

## Riscos residuais

Conforme seção 2.4 do plano: esgotar entregas pode deixar mensagens na DLQ
sem tratamento e saldo divergente; falhas esgotadas no lote da Function podem
perder publicação após avanço do Change Feed. Sem replay/reconstrução
automatizados, não há correção automática. O ledger é a fonte prevista para
reconstrução futura; logs, métricas e alertas são mitigações documentadas,
não validadas por esta execução.
