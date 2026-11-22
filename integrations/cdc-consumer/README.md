# CDC Consumer (demonstrativo)

> **Uso restrito:** este consumidor é apenas demonstrativo (registra mensagens em log) e **não** faz parte do fluxo da solução. Não o execute contra a subscription `consolidado-sub`: ele competiria com o consumidor real do Consolidado e consumiria mensagens sem atualizar o saldo. Use uma subscription exclusiva de diagnóstico, nunca a de produção. Ver tarefa T11 do [plano final](../../docs/implementation/implementation-plan.md).

Consumer Java de demonstração do tópico `lancamentos-topic`. Configure o
namespace do Azure Service Bus pela variável `SERVICE_BUS_NAMESPACE`, no formato
`<namespace>.servicebus.windows.net`, e uma subscription exclusiva pela variável
`SERVICE_BUS_SUBSCRIPTION`. O programa rejeita explicitamente `consolidado-sub`.
A autenticação usa `DefaultAzureCredential`; no Azure, associe uma identidade
com permissão para receber mensagens da subscription exclusiva de diagnóstico.
Este módulo não é incluído em nenhum pipeline de produção.

Com Java 25 (LTS), execute os testes e empacote o JAR executável com:

```bash
mvn clean verify
```

Para executar localmente após configurar `SERVICE_BUS_NAMESPACE`,
`SERVICE_BUS_SUBSCRIPTION` e autenticar com uma credencial suportada pelo Azure
Identity:

```bash
mvn exec:java
```
