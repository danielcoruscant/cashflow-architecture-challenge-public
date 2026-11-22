package br.com.cashflow.lancamentos.configuration;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtSecurityProperties(String issuerUri, String jwkSetUri, String audience) {

    public JwtSecurityProperties {
        requireHttps(issuerUri, "security.jwt.issuer-uri");
        requireHttps(jwkSetUri, "security.jwt.jwk-set-uri");
        if (audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("A propriedade security.jwt.audience é obrigatória.");
        }
    }

    private static void requireHttps(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A propriedade " + property + " é obrigatória.");
        }
        URI uri = URI.create(value);
        if (!uri.isAbsolute() || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("A propriedade " + property + " deve ser uma URI HTTPS absoluta.");
        }
    }
}
