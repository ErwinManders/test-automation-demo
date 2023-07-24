package nl.rabobank.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;

@Configuration
public class CacheConfig
{
    private static final String CACHE_NAME = "cache-name";
    @Value("${hazelcast.address}")
    private String hazelCastNetworkAddress;
    @Bean
    public ClientConfig getClientConfig()
    {
        final var config =  new ClientConfig();
        config.getNetworkConfig()
                .addAddress(hazelCastNetworkAddress);
        return config;
    }

    @Bean
    public HazelcastInstance getHazelCastInstance()
    {
        return HazelcastClient.newHazelcastClient(getClientConfig());
    }

    @Bean
    public CacheManager getCacheManager()
    {
        return new HazelcastCacheManager(getHazelCastInstance());
    }

    @Bean
    public Cache getCache()
    {
        return getCacheManager().getCache(CACHE_NAME);
    }
}
