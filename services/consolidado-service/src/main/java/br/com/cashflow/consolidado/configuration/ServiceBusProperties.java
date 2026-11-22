package br.com.cashflow.consolidado.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "servicebus")
public record ServiceBusProperties(String namespace, String topic, String subscription) {

    public ServiceBusProperties {
        requireText(namespace, "servicebus.namespace");
        requireText(topic, "servicebus.topic");
        requireText(subscription, "servicebus.subscription");
    }

    private static void requireText(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A propriedade " + property + " é obrigatória.");
        }
    }
}
