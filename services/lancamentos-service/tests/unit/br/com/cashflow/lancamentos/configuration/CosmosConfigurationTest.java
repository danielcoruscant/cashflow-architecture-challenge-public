package br.com.cashflow.lancamentos.configuration;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.azure.identity.DefaultAzureCredential;
import org.junit.jupiter.api.Test;

class CosmosConfigurationTest {

    @Test
    void deveUsarDefaultAzureCredential() {
        assertInstanceOf(
                DefaultAzureCredential.class,
                new CosmosConfiguration().cosmosTokenCredential());
    }

    @Test
    void deveRejeitarEndpointOuDatabaseInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new CosmosProperties(null, "fluxocaixa"));
        assertThrows(IllegalArgumentException.class, () -> new CosmosProperties(" ", "fluxocaixa"));
        assertThrows(IllegalArgumentException.class, () -> new CosmosProperties("localhost", "fluxocaixa"));
        assertThrows(IllegalArgumentException.class, () -> new CosmosProperties("https:/missing-host", "fluxocaixa"));
        assertThrows(IllegalArgumentException.class, () -> new CosmosProperties("http://localhost", "fluxocaixa"));
        assertThrows(IllegalArgumentException.class, () -> new CosmosProperties("https://localhost", null));
        assertThrows(IllegalArgumentException.class, () -> new CosmosProperties("https://localhost", " "));
    }
}
