package com.pipeline.medical.repository;

import com.pipeline.medical.model.DocumentChunk;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.index.TextIndexDefinition.TextIndexDefinitionBuilder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class IndexInitializer {
    
    private final MongoTemplate mongoTemplate;
    
    public IndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    @PostConstruct
    public void initIndexes() {
        mongoTemplate.indexOps(DocumentChunk.class)
            .ensureIndex(new TextIndexDefinitionBuilder()
                .onField("topicId")
                .onField("source")
                .onField("content")
                .build());

        System.out.println("Indices de texto creados correctamente");
    }
}
