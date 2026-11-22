package com.fluxocaixa.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import java.util.List;
import org.junit.jupiter.api.Test;

class LancamentoCdcFunctionConfigurationTest {

    @Test
    void triggerDeveUsarConfiguracaoExternaELeasesPreProvisionados() throws Exception {
        CosmosDBTrigger trigger = LancamentoCdcFunction.class
                .getDeclaredMethod("run", List.class, ExecutionContext.class)
                .getParameters()[0]
                .getAnnotation(CosmosDBTrigger.class);

        assertFalse(trigger.createLeaseContainerIfNotExists());
        assertEquals("COSMOS_CONNECTION", trigger.connection());
        assertEquals("%COSMOS_DATABASE%", trigger.databaseName());
        assertEquals("%COSMOS_CONTAINER%", trigger.containerName());
        assertEquals("%COSMOS_LEASE_CONTAINER%", trigger.leaseContainerName());
    }
}
