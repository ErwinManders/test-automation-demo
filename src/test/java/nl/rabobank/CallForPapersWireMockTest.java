package nl.rabobank;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = ConferenceCFPApplication.class
)
class CallForPapersWireMockTest extends CallForPapersHazelCastTest
{
    static final int PORT = 9090;
    protected TestRestTemplate restTemplate = new TestRestTemplate();
    private static WireMockServer wireMockServer;

    @BeforeAll
    static void setUp()
    {
        wireMockServer = new WireMockServer(configure());
        wireMockServer.start();
    }

    static WireMockConfiguration configure()
    {
        return WireMockConfiguration.wireMockConfig()
                .port(PORT)
                .extensions(new WireMockLoginAction())
                .fileSource(new ClasspathFileSource("src/test/resources/testdata"));
    }

    @Test
    @DisplayName("Mock a list of conferences")
    void mockConferencesApi()
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

    @Test
    @DisplayName("Mock an authentication session")
    void mockSession()
    {
        configureFor(PORT);

        stubFor(post(urlMatching("/api/sessions?.*")).willReturn(ok())
                .withPostServeAction(WireMockLoginAction.NAME, emptyMap()));

        URI uri = UriComponentsBuilder
                .fromUriString("http://localhost:9090/api/sessions")
                .queryParam("customerId", "123456789")
                .build()
                .toUri();

        restTemplate.postForEntity(uri, "{}", String.class);

        stubFor(get(urlMatching("/external/api/conferences"))
                .willReturn(
                        aResponse()
                                .withBodyFile("conferences.json")
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withFixedDelay(100)
                                .withStatus(200)));

        final var response = restTemplate.getForEntity("http://localhost:9090/api/conferences", String.class);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(OK);
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
}
