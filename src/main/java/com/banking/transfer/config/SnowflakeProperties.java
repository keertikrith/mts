package com.banking.transfer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds all snowflake.* properties from application-mysql.yml.
 * Values come from environment variables — see .env.example.
 */
@Component
@ConfigurationProperties(prefix = "snowflake")
@Data
public class SnowflakeProperties {
    private String  account;
    private String  user;
    private String  password;
    private String  database;
    private String  schema;
    private String  warehouse;
    private String  role;
    private boolean enabled = true;
}
