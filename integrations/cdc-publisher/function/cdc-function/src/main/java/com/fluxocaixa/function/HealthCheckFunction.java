package com.fluxocaixa.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

public class HealthCheckFunction {

    @FunctionName("healthcheck")
    public HttpResponseMessage run(

            @HttpTrigger(name = "request", methods = {
                    HttpMethod.GET }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<String> request,

            ExecutionContext context) {

        return request
                .createResponseBuilder(HttpStatus.OK)
                .body("Function OK")
                .build();
    }
}