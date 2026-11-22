package br.com.cashflow.lancamentos;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.com.cashflow.lancamentos.interfaces.api.LancamentoController;
import br.com.cashflow.lancamentos.configuration.CosmosProperties;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        classes = LancamentosApplication.class,
        properties = {
            "cosmos.endpoint=https://localhost:8081",
            "cosmos.database=fluxocaixa",
            "security.jwt.issuer-uri=https://login.example.com/tenant/v2.0",
            "security.jwt.jwk-set-uri=https://login.example.com/keys",
            "security.jwt.audience=api://cashflow"
        })
class LancamentosApplicationIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private CosmosClient cosmosClient;

    @MockitoBean
    private CosmosDatabase cosmosDatabase;

    @MockitoBean
    private CosmosContainer cosmosContainer;

    @Test
    void deveIniciarContextoDaAplicacao() {
        assertNotNull(applicationContext.getBean(LancamentoController.class));
        CosmosProperties properties = applicationContext.getBean(CosmosProperties.class);
        org.junit.jupiter.api.Assertions.assertEquals("https://localhost:8081", properties.endpoint());
        org.junit.jupiter.api.Assertions.assertEquals("fluxocaixa", properties.database());
    }
}
