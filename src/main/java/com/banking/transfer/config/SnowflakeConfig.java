package com.banking.transfer.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Creates a Snowflake JDBC Connection bean.
 *
 * Only activated when snowflake.enabled=true (default in mysql profile).
 * Completely skipped in H2 profile where snowflake.enabled=false.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SnowflakeConfig {

    private final SnowflakeProperties props;

    @Bean
    @ConditionalOnProperty(name = "snowflake.enabled", havingValue = "true")
    public Connection snowflakeConnection() throws SQLException {
        // Snowflake JDBC URL format:
        // jdbc:snowflake://<account>.snowflakecomputing.com/
        String url = String.format("jdbc:snowflake://%s.snowflakecomputing.com/", props.getAccount());

        Properties p = new Properties();
        p.put("user",      props.getUser());
        p.put("password",  props.getPassword());
        p.put("db",        props.getDatabase());
        p.put("schema",    props.getSchema());
        p.put("warehouse", props.getWarehouse());
        p.put("role",      props.getRole());
        // Reduce Snowflake driver log noise
        p.put("tracing", "OFF");

        log.info("Connecting to Snowflake: account={}, db={}, schema={}",
                props.getAccount(), props.getDatabase(), props.getSchema());

        Connection conn = DriverManager.getConnection(url, p);
        log.info("Snowflake connection established successfully.");
        return conn;
    }
}
