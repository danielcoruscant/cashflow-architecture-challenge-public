package com.fluxocaixa.function;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluxocaixa.model.LancamentoChangeDocument;
import com.fluxocaixa.model.LancamentoEvent;
import com.fluxocaixa.service.ServiceBusPublisher;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.ExponentialBackoffRetry;
import com.microsoft.azure.functions.annotation.FunctionName;

public class LancamentoCdcFunction {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ServiceBusPublisher publisher;

    // O worker Java instancia esta classe a cada invocação, por isso o publisher é compartilhado.
    public LancamentoCdcFunction() {
        this(ServiceBusPublisher.shared());
    }

    LancamentoCdcFunction(ServiceBusPublisher publisher) {
        this.publisher = publisher;
    }

    // O Cosmos DB trigger não reentrega um lote que falhou; o retry policy do runtime re-executa a
    // invocação e a publicação é idempotente (MessageId + duplicate detection do tópico).
    @FunctionName("cdc-lancamentos")
    @ExponentialBackoffRetry(maxRetryCount = 5, minimumInterval = "00:00:05", maximumInterval = "00:01:00")
    public void run(

            @CosmosDBTrigger(
                    name = "items",
                    connection = "COSMOS_CONNECTION",
                    databaseName = "%COSMOS_DATABASE%",
                    containerName = "%COSMOS_CONTAINER%",
                    leaseContainerName = "%COSMOS_LEASE_CONTAINER%",
                    createLeaseContainerIfNotExists = false)

            List<String> items,

            final ExecutionContext context) {

        Logger logger = context.getLogger();

        logger.info("Processando "
                + items.size()
                + " documentos.");

        int falhas = 0;

        for (String json : items) {

            try {
                LancamentoChangeDocument documento = OBJECT_MAPPER.readValue(
                        json,
                        LancamentoChangeDocument.class);
                LancamentoEvent evento = LancamentoEvent.from(documento);
                String payload = OBJECT_MAPPER.writeValueAsString(evento);

                publisher.publish(
                        evento.eventId(),
                        evento.correlationId(),
                        payload);

                logger.info(
                        "Evento publicado. Id="
                                + evento.eventId()
                                + " CorrelationId="
                                + evento.correlationId());

            } catch (Exception e) {
                falhas++;
                logger.log(Level.SEVERE, "Falha ao publicar documento do change feed.", e);
            }
        }

        if (falhas > 0) {
            throw new IllegalStateException(
                    falhas + " de " + items.size() + " documento(s) não foram publicados.");
        }
    }
}