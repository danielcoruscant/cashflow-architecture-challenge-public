package com.fluxocaixa.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

public class ServiceBusPublisher {

    private static final String TOPIC_NAME = "lancamentos-topic";

    private static final ServiceBusPublisher SHARED = new ServiceBusPublisher();

    private volatile ServiceBusSenderClient sender;

    private ServiceBusPublisher() {
    }

    // O worker Java cria uma nova instância da Function a cada invocação. Compartilhar o publisher
    // evita abrir (e nunca fechar) uma conexão AMQP e uma credencial por invocação.
    public static ServiceBusPublisher shared() {
        return SHARED;
    }

    ServiceBusPublisher(ServiceBusSenderClient sender) {
        this.sender = sender;
    }

    public void publish(String id, String correlationId, String payload) {

        ServiceBusMessage message =
                new ServiceBusMessage(payload);

        message.setMessageId(id);
        message.setCorrelationId(correlationId);

        sender().sendMessage(message);
    }

    // O client é criado sob demanda para que uma configuração ausente apareça no log da invocação.
    private ServiceBusSenderClient sender() {

        ServiceBusSenderClient current = sender;

        if (current == null) {
            synchronized (this) {
                current = sender;
                if (current == null) {
                    current = buildSender();
                    sender = current;
                }
            }
        }

        return current;
    }

    private static ServiceBusSenderClient buildSender() {
        return buildSender(System.getenv("SERVICE_BUS_NAMESPACE"));
    }

    static ServiceBusSenderClient buildSender(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalStateException(
                    "A variável de ambiente SERVICE_BUS_NAMESPACE não está configurada.");
        }

        return new ServiceBusClientBuilder()
                .credential(
                        namespace,
                        new DefaultAzureCredentialBuilder().build())
                .sender()
                .topicName(TOPIC_NAME)
                .buildClient();
    }
}