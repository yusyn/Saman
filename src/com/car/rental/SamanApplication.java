package com.car.rental;

import com.car.rental.db.DatabaseManager;
import com.car.rental.ui.frames.MainFrame;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import javax.swing.SwingUtilities;

/**
 * Single entry point for two runtime modes (Model 2 path):
 *
 * <ul>
 *   <li><b>Desktop (default):</b> Spring context + Swing UI — local Windows use.</li>
 *   <li><b>Server-only:</b> Spring context + HTTP API, no Swing — Ubuntu/Proxmox.</li>
 * </ul>
 *
 * Server-only:
 * <pre>
 *   java -jar saman.jar --server-only
 *   SAMAN_UI_ENABLED=false java -jar saman.jar
 * </pre>
 */
@SpringBootApplication
public class SamanApplication {

    public static void main(String[] args) {
        boolean uiEnabled = resolveUiEnabled(args);

        System.setProperty("java.awt.headless", uiEnabled ? "false" : "true");

        SpringApplicationBuilder builder = new SpringApplicationBuilder(SamanApplication.class)
                .headless(!uiEnabled);

        if (!uiEnabled) {
            builder.properties("saman.ui.enabled=false");
        }

        ConfigurableApplicationContext context = builder.run(args);

        DatabaseManager db = context.getBean(DatabaseManager.class);
        db.initDatabase();

        Environment env = context.getEnvironment();
        String port = env.getProperty("server.port", "8080");

        if (uiEnabled) {
            System.out.println("Saman mode: DESKTOP (Swing UI + API on port " + port + ")");
            SwingUtilities.invokeLater(MainFrame::new);
        } else {
            System.out.println("Saman mode: SERVER-ONLY (no UI). API http://0.0.0.0:" + port + "/api/");
            System.out.println("Try: curl http://127.0.0.1:" + port + "/api/health");
        }
    }

    private static boolean resolveUiEnabled(String[] args) {
        if (args != null) {
            for (String a : args) {
                if (a == null) {
                    continue;
                }
                if ("--server-only".equals(a) || "--saman.ui.enabled=false".equals(a)) {
                    return false;
                }
                if ("--saman.ui.enabled=true".equals(a)) {
                    return true;
                }
            }
        }
        String env = System.getenv("SAMAN_UI_ENABLED");
        if (env != null && !env.isBlank()) {
            return Boolean.parseBoolean(env.trim());
        }
        return true;
    }
}
