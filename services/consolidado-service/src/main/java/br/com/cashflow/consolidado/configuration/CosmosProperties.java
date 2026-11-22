package br.com.cashflow.consolidado.configuration;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cosmos")
public record CosmosProperties(String endpoint, String database) {

    public CosmosProperties {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("A propriedade cosmos.endpoint é obrigatória.");
        }
        URI endpointUri = URI.create(endpoint);
        if (!endpointUri.isAbsolute() || endpointUri.getHost() == null
                || !"https".equalsIgnoreCase(endpointUri.getScheme())) {
            throw new IllegalArgumentException("A propriedade cosmos.endpoint deve ser uma URI HTTPS absoluta.");
        }
        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException("A propriedade cosmos.database é obrigatória.");
        }
    }
}
