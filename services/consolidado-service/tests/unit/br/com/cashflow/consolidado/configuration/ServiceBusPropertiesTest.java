package br.com.cashflow.consolidado.configuration;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ServiceBusPropertiesTest {

    @Test
    void exigeNamespaceTopicoESubscription() {
        assertThrows(IllegalArgumentException.class, () -> new ServiceBusProperties(null, "topic", "sub"));
        assertThrows(IllegalArgumentException.class, () -> new ServiceBusProperties(" ", "topic", "sub"));
        assertThrows(IllegalArgumentException.class, () -> new ServiceBusProperties("ns", null, "sub"));
        assertThrows(IllegalArgumentException.class, () -> new ServiceBusProperties("ns", " ", "sub"));
        assertThrows(IllegalArgumentException.class, () -> new ServiceBusProperties("ns", "topic", null));
        assertThrows(IllegalArgumentException.class, () -> new ServiceBusProperties("ns", "topic", " "));
    }
}
