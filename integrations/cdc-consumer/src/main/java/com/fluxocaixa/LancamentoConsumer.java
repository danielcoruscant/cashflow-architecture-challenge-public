package com.fluxocaixa;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LancamentoConsumer {

    private static final String SERVICE_BUS_NAMESPACE_ENV = "SERVICE_BUS_NAMESPACE";
    private static final String SERVICE_BUS_SUBSCRIPTION_ENV = "SERVICE_BUS_SUBSCRIPTION";
    private static final String TOPIC_NAME = "lancamentos-topic";
    private static final String CONSOLIDADO_SUBSCRIPTION = "consolidado-sub";
    private static final Logger LOGGER = Logger.getLogger(LancamentoConsumer.class.getName());

    public static void main(String[] args) {
        run(
                System.getenv(),
                LancamentoConsumer::createProcessor,
                shutdown -> Runtime.getRuntime().addShutdownHook(
                        new Thread(shutdown, "service-bus-consumer-shutdown")));
    }

    static void run(
            Map<String, String> environment,
            ProcessorFactory processorFactory,
            ShutdownHookRegistrar shutdownHookRegistrar) {
        String namespace = requireNamespace(environment);
        String subscription = requireSubscription(environment);
        ServiceBusProcessorClient processor = processorFactory.create(namespace, subscription);
        CountDownLatch shutdownSignal = new CountDownLatch(1);
        shutdownHookRegistrar.register(shutdownSignal::countDown);

        try {
            processor.start();
            LOGGER.info(() -> "Consumer iniciado para " + TOPIC_NAME + "/"
                    + subscription + " no namespace " + namespace);
            shutdownSignal.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            processor.close();
        }
    }

    static ServiceBusProcessorClient createProcessor(String namespace, String subscription) {
        return new ServiceBusClientBuilder()
                .credential(
                        namespace,
                        new DefaultAzureCredentialBuilder().build())
                .processor()
                .topicName(TOPIC_NAME)
                .subscriptionName(subscription)
                .processMessage(LancamentoConsumer::logMessage)
                .processError(LancamentoConsumer::logError)
                .buildProcessorClient();
    }

    static void logMessage(ServiceBusReceivedMessageContext context) {
        var message = context.getMessage();
        LOGGER.info(() -> "Mensagem recebida. MessageId=" + message.getMessageId());
        LOGGER.info(() -> message.getBody().toString());
    }

    static void logError(ServiceBusErrorContext error) {
        LOGGER.log(
                Level.SEVERE,
                "Erro no processor do Service Bus.",
                error.getException());
    }

    static String requireNamespace(Map<String, String> environment) {
        String namespace = environment.get(SERVICE_BUS_NAMESPACE_ENV);
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalStateException(
                    "A variável de ambiente " + SERVICE_BUS_NAMESPACE_ENV
                            + " não está configurada.");
        }
        return namespace;
    }

    static String requireSubscription(Map<String, String> environment) {
        String configuredSubscription = environment.get(SERVICE_BUS_SUBSCRIPTION_ENV);
        if (configuredSubscription == null || configuredSubscription.isBlank()) {
            throw new IllegalStateException(
                    "A variável de ambiente " + SERVICE_BUS_SUBSCRIPTION_ENV
                            + " não está configurada.");
        }
        String subscription = configuredSubscription.trim();
        if (CONSOLIDADO_SUBSCRIPTION.equals(subscription)) {
            throw new IllegalArgumentException(
                    "O consumidor demonstrativo não pode usar a subscription de produção consolidado-sub.");
        }
        return subscription;
    }

    @FunctionalInterface
    interface ProcessorFactory {
        ServiceBusProcessorClient create(String namespace, String subscription);
    }

    @FunctionalInterface
    interface ShutdownHookRegistrar {
        void register(Runnable shutdown);
    }
}