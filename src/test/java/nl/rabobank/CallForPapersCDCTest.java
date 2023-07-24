package nl.rabobank;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import com.atlassian.ta.wiremockpactgenerator.WireMockPactGenerator;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = ConferenceCFPApplication.class
)
class CallForPapersCDCTest extends CallForPapersHazelCastTest
{
    private static final int PORT = 9090;
    private static WireMockServer wireMockServer;
    protected TestRestTemplate restTemplate = new TestRestTemplate();

    @BeforeAll
    static void setUp()
    {
        wireMockServer = new WireMockServer(configure());
        wireMockServer.start();

        wireMockServer.addMockServiceRequestListener(
                WireMockPactGenerator
                        .builder("ConsumerName", "ProviderName")
                        .build());
    }

    @Test
    @DisplayName("Can create a consumer contract")
    void createConsumerContract()
    {
        configureFor(PORT);

        stubFor(get(urlMatching("/external/api/conferences"))
                .willReturn(
                        aResponse()
                                .withBodyFile("conferences.json")
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withFixedDelay(100)
                                .withStatus(200)));

        final var response = restTemplate.getForEntity("http://localhost:8080/proxy/api/conferences", String.class);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(OK);
        final var headers = response.getHeaders();
        assertThat(headers.getContentType()).isEqualTo(APPLICATION_JSON);
        assertThat(response.getBody()).isEqualToNormalizingWhitespace(
                """
                        [
                           {
                             "name": "JavaCRO",
                             "country": "Croatia",
                             "city": "Rovinj",
                             "date": "17-10-2023"
                           }
                         ]
                        """
        );
    }

    private static WireMockConfiguration configure()
    {
        return WireMockConfiguration.wireMockConfig()
                .port(PORT)
                .fileSource(new ClasspathFileSource("src/test/resources/testdata"));
    }
}
