package nl.rabobank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import nl.rabobank.conference.Conference;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = ConferenceCFPApplication.class
)
@ContextConfiguration(initializers = CallForPapersHazelCastTest.DataSourceInitializer.class)
class CallForPapersHazelCastTest
{
    @Container
    private static final GenericContainer<?> hazelCastContainer =
            new GenericContainer<>(DockerImageName.parse("hazelcast/hazelcast:5.2.0")).withExposedPorts(5701);
    public static class DataSourceInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext>
    {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    "hazelcast.address=" + hazelCastContainer.getHost() + ":" + hazelCastContainer.getFirstMappedPort()
            );
        }
    }

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    @DisplayName("Can test spring cache with test container")
    void testHazelCastCache()
    {
        final var javaCro = Conference.of(
                "JavaCRO", "Croatia", "Rovinj", "17-10-1023");

        final var response = restTemplate.postForEntity("http://localhost:8080/proxy/api/conferences", javaCro, String.class);

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        final String body = response.getBody();
        assertThat(body).isNotNull();
    }

}
