package com.fluxocaixa.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fluxocaixa.service.ServiceBusPublisher;
import com.microsoft.azure.functions.ExecutionContext;

@ExtendWith(MockitoExtension.class)
class LancamentoCdcFunctionTest {

    // Documento do ledger como o change feed entrega, incluindo propriedades de sistema do Cosmos DB.
    private static final String DOCUMENTO_COSMOS = """
            {"id":"teste-001","accountId":"123","tipo":"CREDITO","valor":100.00,\
            "data":"2026-11-20","compensaLancamentoId":null,"correlationId":"corr-001","causationId":"cause-001","_ts":0,\
            "_rid":"ul05ANzP920FAAAAAAAAAA==","_self":"dbs/ul05AA==/colls/ul05ANzP920=/docs/ul05ANzP920FAAAAAAAAAA==/",\
            "_etag":"\\"a50273a5-0000-0300-0000-6b00b3940000\\"","_attachments":"attachments/","_lsn":12}""";
    private static final String EVENTO_JSON = """
            {"eventId":"teste-001","accountId":"123","data":"2026-11-20","tipo":"CREDITO","valor":100.00,\
            "compensaLancamentoId":null,"correlationId":"corr-001","causationId":"cause-001",\
            "timestamp":"1970-01-01T00:00:00Z"}""";

    @Mock
    private ServiceBusPublisher publisher;

    @Mock
    private ExecutionContext context;

    private LancamentoCdcFunction function;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getLogger(LancamentoCdcFunctionTest.class.getName());
        logger.setUseParentHandlers(false);
        when(context.getLogger()).thenReturn(logger);

        function = new LancamentoCdcFunction(publisher);
    }

    @Test
    void devePublicarDocumentoComPropriedadesDeSistemaDoCosmos() {
        function.run(List.of(DOCUMENTO_COSMOS), context);

        verify(publisher).publish("teste-001", "corr-001", EVENTO_JSON);
        verifyNoMoreInteractions(publisher);
    }

    @Test
    void devePublicarTodosOsDocumentosDoLoteNaOrdemRecebida() {
        String segundo = DOCUMENTO_COSMOS.replace("teste-001", "teste-002");

        function.run(List.of(DOCUMENTO_COSMOS, segundo), context);

        InOrder ordem = inOrder(publisher);
        ordem.verify(publisher).publish("teste-001", "corr-001", EVENTO_JSON);
        ordem.verify(publisher).publish(
                "teste-002", "corr-001", EVENTO_JSON.replace("teste-001", "teste-002"));
    }

    @Test
    void naoDeveInteragirComServiceBusQuandoLoteEstaVazio() {
        function.run(List.of(), context);

        verifyNoInteractions(publisher);
    }

    @Test
    void devePublicarOsDemaisDocumentosEFalharQuandoUmNaoTemId() {
        String semId = DOCUMENTO_COSMOS.replace("\"id\":\"teste-001\",", "");

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> function.run(List.of(semId, DOCUMENTO_COSMOS), context));

        assertEquals("1 de 2 documento(s) não foram publicados.", erro.getMessage());
        verify(publisher).publish("teste-001", "corr-001", EVENTO_JSON);
        verifyNoMoreInteractions(publisher);
    }

    @Test
    void deveFalharQuandoIdEstaEmBranco() {
        String idEmBranco = DOCUMENTO_COSMOS.replace("teste-001", "   ");

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> function.run(List.of(idEmBranco), context));

        assertEquals("1 de 1 documento(s) não foram publicados.", erro.getMessage());
        verifyNoInteractions(publisher);
    }

    @Test
    void deveFalharQuandoJsonDoDocumentoEhMalformado() {
        assertThrows(IllegalStateException.class, () -> function.run(List.of("{"), context));

        verifyNoInteractions(publisher);
    }

    @Test
    void deveFalharParaAcionarRetryQuandoPublicacaoNoServiceBusFalha() {
        doThrow(new IllegalStateException("Service Bus indisponível"))
                .when(publisher).publish("teste-001", "corr-001", EVENTO_JSON);

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> function.run(List.of(DOCUMENTO_COSMOS), context));

        assertEquals("1 de 1 documento(s) não foram publicados.", erro.getMessage());
    }

    @Test
    void deveContinuarPublicandoDepoisDeUmaFalhaNoLote() {
        String segundo = DOCUMENTO_COSMOS.replace("teste-001", "teste-002");
        doThrow(new IllegalStateException("Service Bus indisponível"))
                .when(publisher).publish("teste-001", "corr-001", EVENTO_JSON);

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> function.run(List.of(DOCUMENTO_COSMOS, segundo), context));

        assertEquals("1 de 2 documento(s) não foram publicados.", erro.getMessage());
        InOrder ordem = inOrder(publisher);
        ordem.verify(publisher).publish("teste-001", "corr-001", EVENTO_JSON);
        ordem.verify(publisher).publish(
                "teste-002", "corr-001", EVENTO_JSON.replace("teste-001", "teste-002"));
        verifyNoMoreInteractions(publisher);
    }
}
