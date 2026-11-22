package br.com.cashflow.lancamentos.configuration;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CosmosProperties.class)
public class CosmosConfiguration {

    @Bean
    TokenCredential cosmosTokenCredential() {
        return new DefaultAzureCredentialBuilder().build();
    }

    @Bean(destroyMethod = "close")
    CosmosClient cosmosClient(CosmosProperties properties, TokenCredential credential) {
        return new CosmosClientBuilder()
                .endpoint(properties.endpoint())
                .credential(credential)
                .buildClient();
    }

    @Bean
    CosmosDatabase cosmosDatabase(CosmosClient cosmosClient, CosmosProperties properties) {
        return cosmosClient.getDatabase(properties.database());
    }

    @Bean
    CosmosContainer lancamentosContainer(CosmosDatabase cosmosDatabase) {
        return cosmosDatabase.getContainer("lancamentos");
    }
}
