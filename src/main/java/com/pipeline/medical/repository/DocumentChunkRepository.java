package com.pipeline.medical.repository;

import com.pipeline.medical.model.DocumentChunk;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends MongoRepository<DocumentChunk, String> {

    // ── Queries básicas ───────────────────────────────────────────────────

    List<DocumentChunk> findByTopicId(String topicId);

    List<DocumentChunk> findBySource(String source);

    List<DocumentChunk> findByTopicIdOrderByChunkIndex(String topicId);

    long countByTopicId(String topicId);

    // ── Queries para referencias académicas ───────────────────────────────

    /** Todos los chunks de un libro específico, en orden. */
    List<DocumentChunk> findBySourceOrderByChunkIndex(String source);

    /** Busca chunks de una especialidad y año de publicación. */
    List<DocumentChunk> findByTopicIdAndYear(String topicId, String year);

    /** Busca por autor (útil para filtrar por libro). */
    List<DocumentChunk> findByAuthorContainingIgnoreCase(String author);

    /** Busca libros que mencionen una edición específica. */
    List<DocumentChunk> findByEdition(String edition);

    // ── Queries de texto ──────────────────────────────────────────────────

    /**
     * Búsqueda por palabras clave dentro del contenido.
     * Requiere índice de texto en el campo 'content' (definido en DocumentChunk).
     * Uso: repository.searchByKeyword("telencéfalo")
     */
    @Query("{ $text: { $search: ?0 } }")
    List<DocumentChunk> searchByKeyword(String keyword);

    /**
     * Búsqueda de texto dentro de una especialidad.
     * Ideal para el buscador de la app: "buscar 'membrana celular' en Fisiología".
     */
    @Query("{ 'topicId': ?0, $text: { $search: ?1 } }")
    List<DocumentChunk> searchByTopicAndKeyword(String topicId, String keyword);

    /**
     * Devuelve todos los libros únicos (fuentes) de una especialidad.
     * Útil para mostrar la "biblioteca" de un tema.
     */
    @Query(value = "{ 'topicId': ?0 }", fields = "{ 'source': 1, 'bookTitle': 1, 'author': 1, 'edition': 1, 'year': 1, '_id': 0 }")
    List<DocumentChunk> findBooksByTopic(String topicId);

    // ── Estadísticas ──────────────────────────────────────────────────────

    /** Total de chunks por libro (para saber qué tan bien se procesó cada uno). */
    long countBySource(String source);

    /** ¿Ya existe al menos un chunk de este archivo? Evita reprocesados. */
    boolean existsBySource(String source);
}
