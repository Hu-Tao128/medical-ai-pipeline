package com.pipeline.medical.repository;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostgresIndexInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final int expectedEmbeddingDimensions;

    public PostgresIndexInitializer(
            JdbcTemplate jdbcTemplate,
            @org.springframework.beans.factory.annotation.Value("${embedding.vector.dimensions:768}")
            int expectedEmbeddingDimensions
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.expectedEmbeddingDimensions = expectedEmbeddingDimensions;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initIndexes() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS embeddings_topic_idx ON embeddings(topic_id)");
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS embeddings_embedding_idx
                ON embeddings USING ivfflat (embedding vector_cosine_ops)
                WITH (lists = 100)
                """);

        validateEmbeddingColumnDimensions();
    }

    private void validateEmbeddingColumnDimensions() {
        List<String> types = jdbcTemplate.query(
                """
                SELECT format_type(a.atttypid, a.atttypmod)
                FROM pg_attribute a
                JOIN pg_class c ON a.attrelid = c.oid
                WHERE c.relname = 'embeddings'
                  AND a.attname = 'embedding'
                  AND a.attnum > 0
                  AND NOT a.attisdropped
                """,
                (rs, rowNum) -> rs.getString(1)
        );

        if (types.isEmpty()) {
            return;
        }

        String currentType = types.get(0);
        String expectedType = "vector(" + expectedEmbeddingDimensions + ")";

        if (!expectedType.equalsIgnoreCase(currentType)) {
            throw new IllegalStateException(
                    "La columna embeddings.embedding tiene tipo '" + currentType +
                            "' pero la aplicación espera '" + expectedType + "'. " +
                            "Corrige el schema en Supabase. Ejemplo (destructivo): " +
                            "ALTER TABLE embeddings DROP COLUMN embedding; " +
                            "ALTER TABLE embeddings ADD COLUMN embedding " + expectedType + ";"
            );
        }
    }
}
