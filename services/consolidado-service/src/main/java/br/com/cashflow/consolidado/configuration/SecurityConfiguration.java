package br.com.cashflow.consolidado.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class SecurityConfiguration {

        @SuppressWarnings("null")
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                return http
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers("/actuator/health/liveness",
                                                                "/actuator/health/readiness")
                                                .permitAll()
                                                .requestMatchers("/api/v1/consolidado/**")
                                                .hasAuthority("SCOPE_consolidado.read")
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                                .build();
        }

        @Bean
        JwtDecoder jwtDecoder(JwtSecurityProperties properties) {
                NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
                OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(properties.audience())
                                ? OAuth2TokenValidatorResult.success()
                                : OAuth2TokenValidatorResult.failure(
                                                new OAuth2Error("invalid_token", "JWT audience is not accepted.",
                                                                null));
                decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                                JwtValidators.createDefaultWithIssuer(properties.issuerUri()),
                                audienceValidator));
                return decoder;
        }
}
