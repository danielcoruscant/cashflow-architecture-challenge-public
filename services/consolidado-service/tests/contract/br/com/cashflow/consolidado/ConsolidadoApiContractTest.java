package br.com.cashflow.consolidado;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cashflow.consolidado.domain.model.ConsolidadoDiario;
import br.com.cashflow.consolidado.domain.repository.ConsolidadoRepository;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(
        classes = ConsolidadoApplication.class,
        properties = {
            "servicebus.namespace=test.servicebus.windows.net",
            "servicebus.topic=lancamentos-topic",
            "servicebus.subscription=consolidado-sub",
            "cosmos.endpoint=https://localhost:8081",
            "cosmos.database=consolidado",
            "security.jwt.issuer-uri=https://login.example.com/tenant/v2.0",
            "security.jwt.jwk-set-uri=https://login.example.com/keys",
            "security.jwt.audience=api://cashflow"
        })
@AutoConfigureMockMvc
@WithMockUser(authorities = "SCOPE_consolidado.read")
class ConsolidadoApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsolidadoRepository consolidadoRepository;

    @MockitoBean
    private ServiceBusProcessorClient serviceBusProcessorClient;

    @MockitoBean
    private CosmosClient cosmosClient;

    @MockitoBean
    private CosmosDatabase cosmosDatabase;

    @MockitoBean
    private CosmosContainer cosmosContainer;

    @Test
    void getConsolidadoPorDataDeveRetornar200ComCorpoDoContrato() throws Exception {
        LocalDate data = LocalDate.of(2026, 11, 20);
        when(consolidadoRepository.buscarPorData("conta-1", data)).thenReturn(Optional.of(new ConsolidadoDiario(
                "conta-1",
                data, new BigDecimal("100.00"), new BigDecimal("25.00"), new BigDecimal("75.00"))));

        mockMvc.perform(get("/api/v1/consolidado/2026-11-20").param("accountId", "conta-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("conta-1"))
                .andExpect(jsonPath("$.data").value("2026-11-20"))
                .andExpect(jsonPath("$.creditos").value(100.00))
                .andExpect(jsonPath("$.debitos").value(25.00))
                .andExpect(jsonPath("$.saldo").value(75.00));
    }

    @Test
    void getConsolidadoInexistenteDeveRetornar404() throws Exception {
        mockMvc.perform(get("/api/v1/consolidado/2026-11-21").param("accountId", "conta-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CONSOLIDADO_NAO_ENCONTRADO"));
    }

    @Test
    void getConsolidadoComDataInvalidaDeveRetornar400() throws Exception {
        mockMvc.perform(get("/api/v1/consolidado/nao-e-data").param("accountId", "conta-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
    }

    @Test
    @WithAnonymousUser
    void endpointDeConsolidadoDeveExigirAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/consolidado/2026-11-20").param("accountId", "conta-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_lancamentos.read")
    void escopoDeOutroServicoDeveRetornar403() throws Exception {
        mockMvc.perform(get("/api/v1/consolidado/2026-11-20").param("accountId", "conta-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getConsolidadoSemAccountIdDeveRetornar400() throws Exception {
        mockMvc.perform(get("/api/v1/consolidado/2026-11-20"))
                .andExpect(status().isBadRequest());
    }
}
