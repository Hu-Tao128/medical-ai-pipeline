package com.pipeline.medical.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "document_chunks")
@CompoundIndexes({
    @CompoundIndex(name = "topic_source_idx", def = "{'topicId': 1, 'source': 1}"),
    @CompoundIndex(name = "topic_chunk_idx",  def = "{'topicId': 1, 'chunkIndex': 1}")
})
public class DocumentChunk {

    @Id
    private String id;

    @Indexed
    private String topicId;

    @TextIndexed
    private String content;

    private String overlapContent; // Últimos 150 chars del chunk anterior

    @Indexed
    private String source;         // Nombre del archivo (ej. "Guyton Ed.13.pdf")

    private String bookTitle;      // Título extraído de metadatos del PDF
    private String author;         // Autor extraído de metadatos
    private String edition;        // Edición detectada del nombre del archivo
    private String year;           // Año extraído de metadatos o nombre

    private Integer chunkIndex;
    private String  filePath;

    public DocumentChunk() {}

    public DocumentChunk(String topicId, String content, String overlapContent,
                         String source, String bookTitle, String author,
                         String edition, String year,
                         String filePath, Integer chunkIndex) {
        this.topicId        = topicId;
        this.content        = content;
        this.overlapContent = overlapContent;
        this.source         = source;
        this.bookTitle      = bookTitle;
        this.author         = author;
        this.edition        = edition;
        this.year           = year;
        this.filePath       = filePath;
        this.chunkIndex     = chunkIndex;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getId()                            { return id; }
    public void   setId(String id)                   { this.id = id; }

    public String getTopicId()                       { return topicId; }
    public void   setTopicId(String topicId)         { this.topicId = topicId; }

    public String getContent()                       { return content; }
    public void   setContent(String content)         { this.content = content; }

    public String getOverlapContent()                { return overlapContent; }
    public void   setOverlapContent(String v)        { this.overlapContent = v; }

    public String getSource()                        { return source; }
    public void   setSource(String source)           { this.source = source; }

    public String getBookTitle()                     { return bookTitle; }
    public void   setBookTitle(String bookTitle)     { this.bookTitle = bookTitle; }

    public String getAuthor()                        { return author; }
    public void   setAuthor(String author)           { this.author = author; }

    public String getEdition()                       { return edition; }
    public void   setEdition(String edition)         { this.edition = edition; }

    public String getYear()                          { return year; }
    public void   setYear(String year)               { this.year = year; }

    public Integer getChunkIndex()                   { return chunkIndex; }
    public void    setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getFilePath()                      { return filePath; }
    public void   setFilePath(String filePath)       { this.filePath = filePath; }

    @Override
    public String toString() {
        return "DocumentChunk{topicId='" + topicId + "', book='" + bookTitle +
               "', edition='" + edition + "', chunk=" + chunkIndex + "}";
    }
}
