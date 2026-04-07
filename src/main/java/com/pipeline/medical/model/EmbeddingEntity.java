package com.pipeline.medical.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.util.UUID;

@Entity
@Table(name = "embeddings")
public class EmbeddingEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "overlap_content", columnDefinition = "TEXT")
    private String overlapContent;

    @Column(nullable = false)
    private String source;

    @Column(name = "book_title")
    private String bookTitle;

    private String author;

    private String edition;

    private String year;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "content_hash", nullable = false)
    private String contentHash;

    @ColumnTransformer(write = "?::vector", read = "embedding::text")
    @Column(columnDefinition = "vector(768)", nullable = false)
    private String embedding;

    public EmbeddingEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOverlapContent() {
        return overlapContent;
    }

    public void setOverlapContent(String overlapContent) {
        this.overlapContent = overlapContent;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }
}
