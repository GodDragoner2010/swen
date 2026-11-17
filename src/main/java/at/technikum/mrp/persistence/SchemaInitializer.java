package at.technikum.mrp.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

public final class SchemaInitializer {
    private final Database database;

    public SchemaInitializer(Database database) {
        this.database = database;
    }

    public void initialize() throws Exception {
        String sql;
        try (InputStream in = getClass().getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql not found on classpath");
            }
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection c = database.connect()) {
            for (String statement : sql.split(";")) {
                if (statement.isBlank()) {
                    continue;
                }
                try (Statement st = c.createStatement()) {
                    st.execute(statement);
                }
            }
        }
    }
}
