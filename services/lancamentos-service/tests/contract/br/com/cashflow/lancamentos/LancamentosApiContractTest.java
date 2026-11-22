package br.com.cashflow.lancamentos;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cashflow.lancamentos.domain.repository.LancamentoRepository;
import br.com.cashflow.lancamentos.domain.model.PaginaLancamentos;
import br.com.cashflow.lancamentos.domain.model.DataLancamento;
import br.com.cashflow.lancamentos.domain.model.Lancamento;
import br.com.cashflow.lancamentos.domain.model.TipoLancamento;
import br.com.cashflow.lancamentos.domain.model.ValorMonetario;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(classes = LancamentosApplication.class, properties = {
                "cosmos.endpoint=https://localhost:8081",
                "cosmos.database=fluxocaixa",
                "security.jwt.issuer-uri=https://login.example.com/tenant/v2.0",
                "security.jwt.jwk-set-uri=https://login.example.com/keys",
                "security.jwt.audience=api://cashflow"
})
@AutoConfigureMockMvc
@WithMockUser(authorities = { "SCOPE_lancamentos.read", "SCOPE_lancamentos.write" })
class LancamentosApiContractTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private LancamentoRepository lancamentoRepository;

        @MockitoBean
        private CosmosClient cosmosClient;

        @MockitoBean
        private CosmosDatabase cosmosDatabase;

        @MockitoBean
        private CosmosContainer cosmosContainer;

        @SuppressWarnings("null")
        @Test
        void postLancamentosValidoDeveRetornar201ComCorpoDoContrato() throws Exception {
                mockMvc.perform(post("/api/v1/lancamentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountId\":\"conta-1\",\"tipo\":\"CREDITO\",\"valor\":100.00,\"data\":\"2026-11-20\"}"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").isNotEmpty())
                                .andExpect(jsonPath("$.accountId").value("conta-1"))
                                .andExpect(jsonPath("$.tipo").value("CREDITO"))
                                .andExpect(jsonPath("$.valor").value(100.00))
                                .andExpect(jsonPath("$.data").value("2026-11-20"));
        }

        @SuppressWarnings("null")
        @Test
        void postDebitoDevePersistirNoLedgerSemServicoDeConsolidadoNoContexto() throws Exception {
                mockMvc.perform(post("/api/v1/lancamentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountId\":\"conta-1\",\"tipo\":\"DEBITO\",\"valor\":25.00,\"data\":\"2026-11-20\"}"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.accountId").value("conta-1"))
                                .andExpect(jsonPath("$.tipo").value("DEBITO"))
                                .andExpect(jsonPath("$.valor").value(25.00));

                ArgumentCaptor<Lancamento> captor = ArgumentCaptor.forClass(Lancamento.class);
                verify(lancamentoRepository).salvar(captor.capture());
                org.junit.jupiter.api.Assertions.assertEquals(TipoLancamento.DEBITO, captor.getValue().tipo());
        }

        @SuppressWarnings("null")
        @Test
        void postLancamentosSemCamposObrigatoriosDeveRetornar400() throws Exception {
                mockMvc.perform(post("/api/v1/lancamentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"valor\":100.00}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
        }

        @SuppressWarnings("null")
        @Test
        void postLancamentosComTipoForaDoEnumDeveRetornar400() throws Exception {
                mockMvc.perform(post("/api/v1/lancamentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"tipo\":\"OUTRO\",\"valor\":1,\"data\":\"2026-11-20\"}"))
                                .andExpect(status().isBadRequest());
        }

        @SuppressWarnings("null")
        @Test
        void postLancamentosComMaisDeDuasCasasDecimaisDeveRetornar400() throws Exception {
                mockMvc.perform(post("/api/v1/lancamentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountId\":\"conta-1\",\"tipo\":\"CREDITO\",\"valor\":1.001,\"data\":\"2026-11-20\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void getLancamentoInexistenteDeveRetornar404() throws Exception {
                when(lancamentoRepository.buscarPorId("conta-1", "x")).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/v1/lancamentos/x").param("accountId", "conta-1"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.codigo").value("LANCAMENTO_NAO_ENCONTRADO"));
        }

        @Test
        void getLancamentosDeveRetornar200ComPaginaVazia() throws Exception {
                when(lancamentoRepository.listar("conta-1", 0, 20))
                                .thenReturn(new PaginaLancamentos<>(java.util.List.of(), 0, 20, 0));

                mockMvc.perform(get("/api/v1/lancamentos").param("accountId", "conta-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.items").isArray())
                                .andExpect(jsonPath("$.totalItems").value(0))
                                .andExpect(jsonPath("$.totalPages").value(0));
        }

        @Test
        void getLancamentosSemAccountIdDeveRetornar400() throws Exception {
                mockMvc.perform(get("/api/v1/lancamentos"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void postCompensacaoDeveRetornar201EReferenciarLancamentoOriginal() throws Exception {
                when(lancamentoRepository.buscarPorId("conta-1", "original"))
                                .thenReturn(Optional.of(new Lancamento(
                                                "original",
                                                "conta-1",
                                                TipoLancamento.CREDITO,
                                                new ValorMonetario(new BigDecimal("10.00")),
                                                new DataLancamento(LocalDate.of(2026, 11, 20)),
                                                "correlation-1",
                                                "causation-1",
                                                null)));

                mockMvc.perform(post("/api/v1/lancamentos/original/compensacao").param("accountId", "conta-1"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.accountId").value("conta-1"))
                                .andExpect(jsonPath("$.tipo").value("DEBITO"))
                                .andExpect(jsonPath("$.valor").value(10.0))
                                .andExpect(jsonPath("$.compensaLancamentoId").value("original"));
        }

        @Test
        @WithAnonymousUser
        void endpointDeLancamentosDeveExigirAutenticacao() throws Exception {
                mockMvc.perform(get("/api/v1/lancamentos").param("accountId", "conta-1"))
                                .andExpect(status().isUnauthorized());
        }

        @SuppressWarnings("null")
        @Test
        @WithMockUser(authorities = "SCOPE_lancamentos.read")
        void escritaSemEscopoDeGravacaoDeveRetornar403() throws Exception {
                mockMvc.perform(post("/api/v1/lancamentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"accountId\":\"conta-1\",\"tipo\":\"CREDITO\",\"valor\":100.00,\"data\":\"2026-11-20\"}"))
                                .andExpect(status().isForbidden());
        }
}
