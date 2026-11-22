# ADR-013 - Java 25 como Padrão da Solução

## Status

Aceito

## Contexto

Os serviços, a Function e o consumidor demonstrativo precisam de uma versão única de Java. Os quatro projetos Maven já declaram Java 25 e o Azure Functions oferece suporte a Java 25 em disponibilidade geral.

## Decisão

Java 25 (LTS) é o padrão obrigatório de toda a solução, com Spring Boot 3.x e Maven. JDK 25 é usado em desenvolvimento, CI e imagens de contêiner.

## Consequências

### Positivas

- Padrão único em build, CI, imagens e runtime.
- Acesso a recursos recentes da linguagem (records, pattern matching, virtual threads).
- Suporte de longo prazo.

### Negativas

- Bibliotecas e ferramentas precisam ser compatíveis com Java 25; versões devem ser verificadas a cada atualização.
- Menor base de referência da comunidade que versões anteriores.

## Alternativas

Java 21 (LTS anterior): rejeitado por divergir do padrão definido para a entrega.

## Validação

Verificar compatibilidade de Spring Boot, Azure SDKs, OpenAPI Generator, JaCoCo e Testcontainers nos builds (T04, T24).
