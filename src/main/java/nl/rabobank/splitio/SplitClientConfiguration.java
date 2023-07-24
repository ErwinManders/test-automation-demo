package nl.rabobank.splitio;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.split.client.SplitClient;
import io.split.client.SplitClientConfig;
import io.split.client.SplitFactory;
import io.split.client.SplitFactoryBuilder;

@Configuration
public class SplitClientConfiguration
{
    @Value("${split.apiKey}")
    private String apiKey;

    @Bean(name = "splitClient")
    public SplitClient splitClient() throws InterruptedException, IOException, TimeoutException, URISyntaxException
    {
        return buildSplitClient(apiKey);
    }

    private SplitClient buildSplitClient(final String apiKey) throws InterruptedException, TimeoutException, IOException, URISyntaxException
    {
        SplitClientConfig config = SplitClientConfig.builder()
                .setBlockUntilReadyTimeout(20000)
                .enableDebug()
                .build();

        SplitFactory splitFactory = SplitFactoryBuilder.build(apiKey, config);
        SplitClient client = splitFactory.client();
        client.blockUntilReady();
        return client;
    }
}
