package com.fluxocaixa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LancamentoConsumerTest {

    @Test
    void deveRetornarNamespaceConfigurado() {
        String namespace = LancamentoConsumer.requireNamespace(
                Map.of("SERVICE_BUS_NAMESPACE", "cashflow.servicebus.windows.net"));

        assertEquals("cashflow.servicebus.windows.net", namespace);
    }

    @Test
    void deveFalharQuandoNamespaceNaoEstaConfigurado() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> LancamentoConsumer.requireNamespace(Map.of()));

        assertEquals(
                "A variável de ambiente SERVICE_BUS_NAMESPACE não está configurada.",
                error.getMessage());
    }

    @Test
    void deveFalharQuandoNamespaceEstaEmBranco() {
        assertThrows(
                IllegalStateException.class,
                () -> LancamentoConsumer.requireNamespace(
                        Map.of("SERVICE_BUS_NAMESPACE", "  ")));
    }

    @Test
    void deveExigirSubscriptionExclusiva() {
        String subscription = LancamentoConsumer.requireSubscription(
                Map.of("SERVICE_BUS_SUBSCRIPTION", "diagnostico-local"));

        assertEquals("diagnostico-local", subscription);
    }

    @Test
    void deveFalharQuandoSubscriptionNaoEstaConfigurada() {
        assertThrows(IllegalStateException.class, () -> LancamentoConsumer.requireSubscription(Map.of()));
    }

    @Test
    void deveImpedirUsoDaSubscriptionDeProducao() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> LancamentoConsumer.requireSubscription(
                        Map.of("SERVICE_BUS_SUBSCRIPTION", "consolidado-sub")));

        assertEquals(
                "O consumidor demonstrativo não pode usar a subscription de produção consolidado-sub.",
                error.getMessage());
    }

    @Test
    void deveImpedirSubscriptionDeProducaoComEspacos() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LancamentoConsumer.requireSubscription(
                        Map.of("SERVICE_BUS_SUBSCRIPTION", "  consolidado-sub  ")));
    }
}
