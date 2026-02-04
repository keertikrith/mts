package com.banking.transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MoneyTransferSystemApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MoneyTransferSystemApplication.class, args);
    }
}
