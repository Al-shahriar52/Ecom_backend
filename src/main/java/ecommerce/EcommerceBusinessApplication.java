package ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableJpaAuditing
@SpringBootApplication
@EnableCaching
@EnableAsync
public class EcommerceBusinessApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceBusinessApplication.class, args);
    }

}
