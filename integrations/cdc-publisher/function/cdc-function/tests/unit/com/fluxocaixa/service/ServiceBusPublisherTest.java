package com.fluxocaixa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

@ExtendWith(MockitoExtension.class)
class ServiceBusPublisherTest {

    @Mock
    private ServiceBusSenderClient sender;

    @Test
    void publishPropagaCorrelationIdComoMetadadoDoServiceBus() {
        ServiceBusPublisher publisher = new ServiceBusPublisher(sender);

        publisher.publish("event-1", "correlation-1", "{\"eventId\":\"event-1\"}");

        ArgumentCaptor<ServiceBusMessage> captor = ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(sender).sendMessage(captor.capture());
        assertEquals("event-1", captor.getValue().getMessageId());
        assertEquals("correlation-1", captor.getValue().getCorrelationId());
    }
}
