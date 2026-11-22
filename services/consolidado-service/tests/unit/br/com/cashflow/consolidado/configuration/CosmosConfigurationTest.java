package br.com.cashflow.consolidado.configuration;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.azure.identity.DefaultAzureCredential;
import org.junit.jupiter.api.Test;

class CosmosConfigurationTest {

    @Test
    void usaDefaultAzureCredentialSemChaveEstatica() {
        assertInstanceOf(
                DefaultAzureCredential.class,
                new CosmosConfiguration().cosmosTokenCredential());
    }

    @Test
    void rejeitaEndpointInseguroOuDatabaseAusente() {
        assertThrows(IllegalArgumentException.class, () -> new CosmosProperties(null, "consolidado"));
        assertThrows(IllegalArgumentException.class, () -> new CosmosProperties(" ", "consolidado"));
        assertThrows(IllegalArgumentException.class, () -> new CosmosProperties("localhost", "consolidado"));
        assertThrows(IllegalArgumentException.class, () -> new CosmosProperties("https:/missing-host", "consolidado"));
        assertThrows(IllegalArgumentException.class, () -> new CosmosProperties("http://localhost", "consolidado"));
        assertThrows(IllegalArgumentException.class, () -> new CosmosProperties("https://localhost", null));
        assertThrows(IllegalArgumentException.class, () -> new CosmosProperties("https://localhost", " "));
    }
}
