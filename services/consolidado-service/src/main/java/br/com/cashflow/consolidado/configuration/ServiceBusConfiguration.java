package br.com.cashflow.consolidado.configuration;

import br.com.cashflow.consolidado.infrastructure.servicebus.LancamentoCreatedMessageHandler;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServiceBusProperties.class)
public class ServiceBusConfiguration {

    private static final Logger LOGGER = Logger.getLogger(ServiceBusConfiguration.class.getName());

    @Bean(initMethod = "start", destroyMethod = "close")
    ServiceBusProcessorClient serviceBusProcessorClient(
            ServiceBusProperties properties,
            LancamentoCreatedMessageHandler messageHandler) {
        return new ServiceBusClientBuilder()
                .credential(properties.namespace(), new DefaultAzureCredentialBuilder().build())
                .processor()
                .topicName(properties.topic())
                .subscriptionName(properties.subscription())
                .disableAutoComplete()
                .maxAutoLockRenewDuration(Duration.ofMinutes(5))
                .processMessage(messageHandler::process)
                .processError(error -> LOGGER.log(
                        Level.SEVERE,
                        "Falha no processor do Service Bus.",
                        error.getException()))
                .buildProcessorClient();
    }
}
