package br.com.cashflow.consolidado;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.com.cashflow.consolidado.interfaces.api.ConsolidadoController;
import br.com.cashflow.consolidado.configuration.CosmosProperties;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        classes = ConsolidadoApplication.class,
        properties = {
            "servicebus.namespace=test.servicebus.windows.net",
            "servicebus.topic=lancamentos-topic",
            "servicebus.subscription=consolidado-sub",
            "cosmos.endpoint=https://localhost:8081",
            "cosmos.database=consolidado",
            "security.jwt.issuer-uri=https://login.example.com/tenant/v2.0",
            "security.jwt.jwk-set-uri=https://login.example.com/keys",
            "security.jwt.audience=api://cashflow"
        })
class ConsolidadoApplicationIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private ServiceBusProcessorClient serviceBusProcessorClient;

    @MockitoBean
    private CosmosClient cosmosClient;

    @MockitoBean
    private CosmosDatabase cosmosDatabase;

    @MockitoBean
    private CosmosContainer cosmosContainer;

    @Test
    void deveIniciarContextoDaAplicacao() {
        assertNotNull(applicationContext.getBean(ConsolidadoController.class));
        assertNotNull(applicationContext.getBean(CosmosProperties.class));
    }
}
