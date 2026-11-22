# Estratégia de teste de carga

## Objetivo e limites

Este plano define como verificar os requisitos NFR-003 e NFR-004 em um ambiente de teste. Os alvos são 50 requisições/s no Consolidado, perda máxima de 5%, P95 abaixo de 200 ms para consultas e P95 abaixo de 500 ms para escritas. **São metas; nenhum resultado de carga foi medido nesta entrega.**

Não execute contra produção. Cosmos DB, Service Bus, identidade e escala das Container Apps precisam ser provisionados em uma assinatura de teste antes da medição. O custo de infraestrutura e a geração de dados devem ser aprovados antes do teste.

## Ferramenta e perfil

Utilizar k6 com cenários separados para leitura e escrita:

1. **Consulta de consolidado:** GET do saldo atual e por data, token válido com `consolidado.read`, contas e datas distribuídas entre dados previamente criados.
2. **Registro de lançamento:** POST de créditos/débitos com identificadores únicos e token `lancamentos.write`; limpar apenas a conta/ambiente descartável após a execução, nunca editar o ledger.
3. **Fluxo assíncrono:** correlacionar os IDs de lançamentos aceitos com as projeções observadas; medir o atraso de consistência eventual separadamente do tempo de resposta HTTP.

Fases por cenário:

| Fase | Duração | Carga |
|---|---:|---:|
| Aquecimento | 5 min | 5 req/s |
| Rampa | 10 min | 5 → 50 req/s |
| Sustentação | 30 min | 50 req/s |
| Pico | 5 min | 75 req/s |
| Recuperação | 5 min | 5 req/s |

Executar primeiro cada cenário isoladamente e depois em conjunto. Registrar a proporção de leitura/escrita, concorrência, região, SKUs, limites de escala, throughput RU/s, tamanho do conjunto de dados e versão dos artefatos.

## Métricas e fórmulas

- **Taxa oferecida:** número de iterações/inícios de requisição dividido pela duração; confirmar que a carga efetiva atingiu 50 req/s durante a sustentação.
- **Perda de requisições (%):** `100 × (requisições esperadas − respostas válidas recebidas) / requisições esperadas`. Falha funcional ou resposta HTTP não esperada conta como inválida.
- **P95 HTTP:** percentil 95 da duração de resposta por endpoint e método, excluindo apenas falhas de infraestrutura do gerador documentadas; reportar erros separadamente, sem removê-los do cálculo de perda.
- **Atraso de projeção:** `instante em que o saldo esperado fica consultável − instante de aceitação do lançamento`. Relatar P50, P95 e máximo; não somar esse atraso à latência HTTP.

## Critérios de aceite e evidências

O cenário Consolidado passa quando sustentação mantém 50 req/s, perda ≤ 5% e P95 < 200 ms. O endpoint de escrita passa quando P95 < 500 ms. As asserções funcionais devem confirmar saldos, partições por conta/data e ausência de aplicação duplicada.

Guardar comando, versão do k6, configuração, dados de entrada não sensíveis, logs e relatório bruto. Repetir pelo menos três vezes após aquecimento dos serviços e publicar cada execução; não reportar somente a melhor. Diferenças de SKU, ambiente ou conjunto de dados invalidam a comparação direta.

## Estado desta entrega

Estratégia documentada; teste de carga não executado. Não há evidência de 50 req/s, P95 ou perda para esta solução.
