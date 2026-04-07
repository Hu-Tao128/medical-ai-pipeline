package com.pipeline.medical.repository;

import com.pipeline.medical.model.EmbeddingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmbeddingRepository extends JpaRepository<EmbeddingEntity, UUID> {

    List<EmbeddingEntity> findByTopicId(UUID topicId);

    List<EmbeddingEntity> findBySource(String source);

    List<EmbeddingEntity> findByTopicIdOrderByChunkIndex(UUID topicId);

    long countByTopicId(UUID topicId);

    List<EmbeddingEntity> findBySourceOrderByChunkIndex(String source);

    List<EmbeddingEntity> findByTopicIdAndYear(UUID topicId, String year);

    List<EmbeddingEntity> findByAuthorContainingIgnoreCase(String author);

    List<EmbeddingEntity> findByEdition(String edition);

    @Query(value = "SELECT * FROM embeddings WHERE content ILIKE CONCAT('%', :keyword, '%')", nativeQuery = true)
    List<EmbeddingEntity> searchByKeyword(@Param("keyword") String keyword);

    @Query(value = "SELECT * FROM embeddings WHERE topic_id = :topicId AND content ILIKE CONCAT('%', :keyword, '%')",
            nativeQuery = true)
    List<EmbeddingEntity> searchByTopicAndKeyword(@Param("topicId") UUID topicId, @Param("keyword") String keyword);

    @Query(value = "SELECT DISTINCT ON (source) * FROM embeddings WHERE topic_id = :topicId ORDER BY source, chunk_index",
            nativeQuery = true)
    List<EmbeddingEntity> findBooksByTopic(@Param("topicId") UUID topicId);

    long countBySource(String source);

    boolean existsBySource(String source);

    @Query(value = """
            SELECT *
            FROM embeddings
            WHERE topic_id = :topicId
            ORDER BY embedding <-> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<EmbeddingEntity> findTopKSimilar(
            @Param("topicId") UUID topicId,
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );

    default List<EmbeddingEntity> semanticSearch(UUID topicId, float[] embedding, int limit) {
        return findTopKSimilar(topicId, toVectorLiteral(embedding), limit);
    }

    private static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
