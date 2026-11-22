package com.fluxocaixa.function;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;

@ExtendWith(MockitoExtension.class)
class HealthCheckFunctionTest {

    @Mock
    private HttpRequestMessage<String> request;

    @Mock
    private HttpResponseMessage.Builder responseBuilder;

    @Mock
    private HttpResponseMessage response;

    @Mock
    private ExecutionContext context;

    @Test
    void deveResponderOkComMensagemDeSaude() {
        when(request.createResponseBuilder(HttpStatus.OK)).thenReturn(responseBuilder);
        when(responseBuilder.body("Function OK")).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);

        HttpResponseMessage actual = new HealthCheckFunction().run(request, context);

        assertSame(response, actual);
        verify(responseBuilder).body("Function OK");
    }
}
