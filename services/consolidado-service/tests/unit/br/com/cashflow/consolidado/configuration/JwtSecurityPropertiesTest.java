package br.com.cashflow.consolidado.configuration;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JwtSecurityPropertiesTest {

    private static final String ISSUER = "https://login.example.com/tenant";
    private static final String JWK_SET = "https://login.example.com/keys";

    @Test
    void aceitaUrisHttpsEAudienceValidas() {
        new JwtSecurityProperties(ISSUER, JWK_SET, "api://cashflow");
    }

    @Test
    void rejeitaIssuerAusenteRelativoSemHostOuSemHttps() {
        assertThrows(IllegalArgumentException.class, () -> new JwtSecurityProperties(null, JWK_SET, "api"));
        assertThrows(IllegalArgumentException.class, () -> new JwtSecurityProperties(" ", JWK_SET, "api"));
        assertThrows(IllegalArgumentException.class, () -> new JwtSecurityProperties("relative", JWK_SET, "api"));
        assertThrows(IllegalArgumentException.class, () -> new JwtSecurityProperties("mailto:user@example.com", JWK_SET, "api"));
        assertThrows(IllegalArgumentException.class, () -> new JwtSecurityProperties("http://login.example.com", JWK_SET, "api"));
    }

    @Test
    void rejeitaJwkSetAusenteOuUriInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new JwtSecurityProperties(ISSUER, null, "api"));
        assertThrows(IllegalArgumentException.class, () -> new JwtSecurityProperties(ISSUER, " ", "api"));
        assertThrows(IllegalArgumentException.class, () -> new JwtSecurityProperties(ISSUER, "relative", "api"));
        assertThrows(IllegalArgumentException.class, () -> new JwtSecurityProperties(
                ISSUER, "mailto:user@example.com", "api"));
        assertThrows(IllegalArgumentException.class, () -> new JwtSecurityProperties(
                ISSUER, "http://login.example.com/keys", "api"));
    }

    @Test
    void rejeitaAudienceAusenteOuEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> new JwtSecurityProperties(ISSUER, JWK_SET, null));
        assertThrows(IllegalArgumentException.class, () -> new JwtSecurityProperties(ISSUER, JWK_SET, " "));
    }
}
