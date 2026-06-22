package com.banking.transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync               // Required for @Async in SnowflakeSyncService
@ConfigurationPropertiesScan  // Picks up SnowflakeProperties @ConfigurationProperties
public class MoneyTransferSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoneyTransferSystemApplication.class, args);
    }
}
