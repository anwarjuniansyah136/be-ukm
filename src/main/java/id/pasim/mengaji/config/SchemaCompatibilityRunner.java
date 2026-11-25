package id.pasim.mengaji.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Ensures existing MySQL/H2 databases allow NULL for users.role and users.requested_role
 * so newly registered accounts can remain without a role until approved by Admin/Pengurus.
 */
@Component
public class SchemaCompatibilityRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SchemaCompatibilityRunner.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public SchemaCompatibilityRunner(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName().toLowerCase();
            if (product.contains("mysql") || product.contains("mariadb")) {
                executeQuietly("ALTER TABLE users MODIFY COLUMN role VARCHAR(32) NULL");
                executeQuietly("ALTER TABLE users MODIFY COLUMN requested_role VARCHAR(32) NULL");
            } else if (product.contains("h2")) {
                executeQuietly("ALTER TABLE users ALTER COLUMN role VARCHAR(32) NULL");
                executeQuietly("ALTER TABLE users ALTER COLUMN requested_role VARCHAR(32) NULL");
            } else if (product.contains("postgresql")) {
                executeQuietly("ALTER TABLE users ALTER COLUMN role DROP NOT NULL");
                executeQuietly("ALTER TABLE users ALTER COLUMN requested_role DROP NOT NULL");
            }
        } catch (Exception ex) {
            log.debug("Lewati penyesuaian skema kolom role nullable: {}", ex.getMessage());
        }
    }

    private void executeQuietly(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ex) {
            log.debug("SQL kompatibilitas dilewati ({}): {}", sql, ex.getMessage());
        }
    }
}
