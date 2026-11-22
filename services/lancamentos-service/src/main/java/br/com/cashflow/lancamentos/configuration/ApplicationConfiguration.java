package br.com.cashflow.lancamentos.configuration;

import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer jsonNullableModule() {
        return builder -> builder.modulesToInstall(new JsonNullableModule());
    }
}
