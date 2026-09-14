package com.car.rental;

import com.car.rental.db.SchemaInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * HTTP API + static Web UI entry point (no desktop Swing).
 *
 * <pre>
 *   mvn spring-boot:run
 *   java -jar target/saman-1.0.0-SNAPSHOT.jar
 * </pre>
 *
 * Browser: {@code http://127.0.0.1:8080/}
 * API:     {@code http://127.0.0.1:8080/api/}
 */
@SpringBootApplication
public class SamanApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        ConfigurableApplicationContext context = new SpringApplicationBuilder(SamanApplication.class)
                .headless(true)
                .run(args);

        SchemaInitializer schema = context.getBean(SchemaInitializer.class);
        schema.initDatabase();

        Environment env = context.getEnvironment();
        String port = env.getProperty("server.port", "8080");
        System.out.println("Saman SERVER mode");
        System.out.println("  Web UI: http://0.0.0.0:" + port + "/");
        System.out.println("  API:    http://0.0.0.0:" + port + "/api/");
        System.out.println("  Health: curl http://127.0.0.1:" + port + "/api/health");
    }
}
