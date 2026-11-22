# ADR-017 - Autorização por Operação e Contas Compartilhadas

## Status

Aceito

## Contexto

A solução atende múltiplos comerciantes, identificados por `accountId`. Por decisão de negócio, todo usuário autenticado e autorizado pode acessar qualquer conta; a autenticação e a autorização servem para validar as **permissões do usuário** (por exemplo, leitura e gravação), não para isolar dados entre comerciantes.

## Decisão

- Autenticação com Microsoft Entra ID (OIDC, Authorization Code + PKCE na SPA).
- Os serviços são *Resource Servers* JWT: validam assinatura, emissor, audiência e expiração.
- Autorização por operação, com escopos/papéis (leitura de lançamentos, gravação de lançamentos, leitura do consolidado).
- O APIM aplica validação de JWT e rate limit; os serviços validam novamente (defesa em profundidade).
- Não existe isolamento de dados por comerciante.

## Consequências

### Positivas

- Modelo simples, compatível com o requisito de negócio.
- Menor privilégio por operação.

### Negativas

- Um usuário com permissão de leitura pode consultar qualquer conta; o risco é aceito e deve ser comunicado.
- Evolução para isolamento por conta exigirá claims de conta, filtros e testes adicionais.

## Alternativas

Isolamento por comerciante (claims por conta): fora do escopo por decisão de negócio.
