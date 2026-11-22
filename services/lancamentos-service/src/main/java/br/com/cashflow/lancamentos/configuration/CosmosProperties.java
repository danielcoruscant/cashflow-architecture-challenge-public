package br.com.cashflow.lancamentos.configuration;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cosmos")
public record CosmosProperties(String endpoint, String database) {

    public CosmosProperties {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("A propriedade cosmos.endpoint é obrigatória.");
        }
        URI endpointUri = URI.create(endpoint);
        if (!endpointUri.isAbsolute() || endpointUri.getHost() == null) {
            throw new IllegalArgumentException("A propriedade cosmos.endpoint deve ser uma URI absoluta.");
        }
        if (!"https".equalsIgnoreCase(endpointUri.getScheme())) {
            throw new IllegalArgumentException("A propriedade cosmos.endpoint deve usar HTTPS.");
        }
        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException("A propriedade cosmos.database é obrigatória.");
        }
    }
}
