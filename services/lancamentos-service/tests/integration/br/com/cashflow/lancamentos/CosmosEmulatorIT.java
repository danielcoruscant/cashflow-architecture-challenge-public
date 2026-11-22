package br.com.cashflow.lancamentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cashflow.lancamentos.domain.model.DataLancamento;
import br.com.cashflow.lancamentos.domain.model.Lancamento;
import br.com.cashflow.lancamentos.domain.model.TipoLancamento;
import br.com.cashflow.lancamentos.domain.model.ValorMonetario;
import br.com.cashflow.lancamentos.infrastructure.cosmos.CosmosLancamentoRepository;
import br.com.cashflow.lancamentos.infrastructure.mapper.LancamentoDocumentMapper;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.GatewayConnectionConfig;
import com.azure.cosmos.models.CosmosContainerProperties;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class CosmosEmulatorIT {

    private static final String EMULATOR_IMAGE = "mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator@sha256:09dcd04e7149975f0427e6cb09781681f407fc74d01e54fc9d6410c36795cda3";
    private static final String EMULATOR_KEY = "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==";
    private static final char[] TRUST_STORE_PASSWORD = UUID.randomUUID().toString().toCharArray();

    @SuppressWarnings("resource")
    @Container
    private static final GenericContainer<?> EMULATOR = new GenericContainer<>(DockerImageName.parse(EMULATOR_IMAGE))
            .withEnv("PROTOCOL", "https")
            .withEnv("ENABLE_EXPLORER", "false")
            .withEnv("ENABLE_TELEMETRY", "false")
            .withExposedPorts(8080, 8081)
            .waitingFor(Wait.forHttp("/ready")
                    .forPort(8080)
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(5)));

    private static CosmosClient cosmosClient;
    private static CosmosLancamentoRepository repository;
    private static String databaseId;
    private static Path trustStorePath;
    private static String previousTrustStore;
    private static String previousTrustStorePassword;
    private static String previousTrustStoreType;
    private static SSLContext previousSslContext;

    @BeforeAll
    static void conectarAoEmulador() throws Exception {
        previousTrustStore = System.getProperty("javax.net.ssl.trustStore");
        previousTrustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
        previousTrustStoreType = System.getProperty("javax.net.ssl.trustStoreType");
        previousSslContext = SSLContext.getDefault();
        configurarTrustStoreDoEmulador();

        String endpoint = "https://localhost:" + EMULATOR.getMappedPort(8081);
        cosmosClient = new CosmosClientBuilder()
                .endpoint(endpoint)
                .key(EMULATOR_KEY)
                .gatewayMode(new GatewayConnectionConfig())
                .buildClient();

        databaseId = "lancamentos-it-" + UUID.randomUUID();
        cosmosClient.createDatabaseIfNotExists(databaseId);
        var database = cosmosClient.getDatabase(databaseId);
        database.createContainerIfNotExists(new CosmosContainerProperties("lancamentos", "/accountId"));
        CosmosContainer container = database.getContainer("lancamentos");
        repository = new CosmosLancamentoRepository(container, new LancamentoDocumentMapper());
    }

    @AfterAll
    static void limparRecursos() throws Exception {
        try {
            if (cosmosClient != null) {
                try {
                    if (databaseId != null) {
                        cosmosClient.getDatabase(databaseId).delete();
                    }
                } finally {
                    cosmosClient.close();
                }
            }
        } finally {
            try {
                EMULATOR.close();
            } finally {
                restaurarTrustStore();
            }
        }
    }

    @Test
    void persisteEConsultaLancamentoNaParticaoDaConta() {
        Lancamento lancamento = new Lancamento(
                "lancamento-it-1",
                "conta-it-1",
                TipoLancamento.CREDITO,
                new ValorMonetario(new BigDecimal("10.00")),
                new DataLancamento(LocalDate.of(2026, 11, 21)),
                "correlation-it-1",
                "causation-it-1",
                null);

        repository.salvar(lancamento);

        Lancamento encontrado = repository.buscarPorId("conta-it-1", "lancamento-it-1").orElseThrow();
        assertEquals(lancamento.id(), encontrado.id());
        assertEquals(lancamento.accountId(), encontrado.accountId());
        assertEquals(lancamento.tipo(), encontrado.tipo());
        assertEquals(0, lancamento.valor().valor().compareTo(encontrado.valor().valor()));
        assertEquals(lancamento.data(), encontrado.data());
        assertTrue(repository.buscarPorId("outra-conta-it", lancamento.id()).isEmpty());
    }

    private static void configurarTrustStoreDoEmulador() throws Exception {
        X509Certificate certificate = obterCertificadoEmulador();
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, TRUST_STORE_PASSWORD);
        trustStore.setCertificateEntry("cosmos-emulator", certificate);

        trustStorePath = Files.createTempFile("cashflow-cosmos-emulator-", ".p12");
        try (OutputStream output = Files.newOutputStream(trustStorePath)) {
            trustStore.store(output, TRUST_STORE_PASSWORD);
        }

        System.setProperty("javax.net.ssl.trustStore", trustStorePath.toString());
        System.setProperty("javax.net.ssl.trustStorePassword", new String(TRUST_STORE_PASSWORD));
        System.setProperty("javax.net.ssl.trustStoreType", trustStore.getType());
        TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
        SSLContext.setDefault(sslContext);
    }

    private static X509Certificate obterCertificadoEmulador() throws Exception {
        // Fixa o certificado autoassinado do emulador antes da conexão do SDK.
        TrustManager[] trustAllForCertificateDiscovery = {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        SSLContext certificateDiscoveryContext = SSLContext.getInstance("TLS");
        certificateDiscoveryContext.init(null, trustAllForCertificateDiscovery, new SecureRandom());
        try (Socket socket = certificateDiscoveryContext
                .getSocketFactory()
                .createSocket("localhost", EMULATOR.getMappedPort(8081))) {
            ((SSLSocket) socket).startHandshake();
            return (X509Certificate) ((SSLSocket) socket).getSession().getPeerCertificates()[0];
        }
    }

    private static void restaurarTrustStore() throws Exception {
        restaurarPropriedade("javax.net.ssl.trustStore", previousTrustStore);
        restaurarPropriedade("javax.net.ssl.trustStorePassword", previousTrustStorePassword);
        restaurarPropriedade("javax.net.ssl.trustStoreType", previousTrustStoreType);
        if (previousSslContext != null) {
            SSLContext.setDefault(previousSslContext);
        }
        if (trustStorePath != null) {
            Files.deleteIfExists(trustStorePath);
        }
    }

    private static void restaurarPropriedade(String nome, String valorAnterior) {
        if (valorAnterior == null) {
            System.clearProperty(nome);
        } else {
            System.setProperty(nome, valorAnterior);
        }
    }
}
