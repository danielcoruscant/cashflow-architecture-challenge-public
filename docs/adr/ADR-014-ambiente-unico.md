# ADR-014 - Ambiente Único

## Status

Aceito

## Contexto

O desafio prioriza decisões arquiteturais e a solução de negócio. Manter vários ambientes aumentaria a complexidade de Terraform, pipelines e configuração sem acrescentar valor demonstrável.

## Decisão

Implementar apenas um ambiente alvo, `prod`:

```text
infra/
 └── environments/
      └── prod/
```

O pipeline de deploy atende somente esse ambiente. Nenhum recurso Azure é provisionado nesta entrega; o Terraform é validado estaticamente.

## Consequências

### Positivas

- Menor esforço e menor superfície de erro.
- Foco no fluxo de negócio e na integração.

### Negativas

- Sem ambientes para validação prévia de mudanças.
- Sem estratégia de promoção implementada.
- Parte do dimensionamento de produção (por exemplo, APIM Developer sem SLA) é uma simplificação consciente.

## Evolução futura

Dev, Homologação, QA e Sandbox, com promoção imutável de artefatos, GitHub Environments com aprovação e composições Terraform por ambiente.
