package rw.ac.rca.isoko;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import rw.ac.rca.isoko.config.IsokoProperties;

@SpringBootApplication(exclude = {
        UserDetailsServiceAutoConfiguration.class,
        DataRedisRepositoriesAutoConfiguration.class
})
@EnableConfigurationProperties(IsokoProperties.class)
public class IsokoApplication {

    public static void main(String[] args) {
        SpringApplication.run(IsokoApplication.class, args);
    }
}
