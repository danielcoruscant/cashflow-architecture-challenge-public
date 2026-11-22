package br.com.cashflow.consolidado.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    public OpenAPI consolidadoOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Cash Flow - Consolidado API")
                .version("v1")
                .description("API responsável pela consulta dos saldos consolidados financeiros."));
    }
}
