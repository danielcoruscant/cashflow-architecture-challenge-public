package com.fluxocaixa.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.Test;

class ServiceBusPublisherConfigurationTest {

    @Test
    void deveExigirNamespaceConfigurado() {
        assertThrows(IllegalStateException.class, () -> ServiceBusPublisher.buildSender(null));
        assertThrows(IllegalStateException.class, () -> ServiceBusPublisher.buildSender("  "));
    }

    @Test
    void deveCriarSenderComNamespaceConfigurado() {
        ServiceBusSenderClient sender = ServiceBusPublisher.buildSender("cashflow.servicebus.windows.net");

        assertNotNull(sender);
        sender.close();
    }
}
