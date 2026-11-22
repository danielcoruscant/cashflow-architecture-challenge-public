package br.com.cashflow.lancamentos.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    public OpenAPI lancamentosOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Cash Flow - Lancamentos API")
                .version("v1")
                .description("API responsável pelo registro e consulta dos lançamentos financeiros."));
    }
}
