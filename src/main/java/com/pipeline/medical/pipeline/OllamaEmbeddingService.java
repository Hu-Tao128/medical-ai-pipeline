package com.pipeline.medical.pipeline;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OllamaEmbeddingService implements EmbeddingGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingService.class);
    private static final int EMBEDDING_DIMENSION = 768;

    private final RestClient restClient;
    private final boolean embeddingsEnabled;
    private final String model;
    private final String actionModel;

    public OllamaEmbeddingService(
            @Value("${embedding.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${embedding.ollama.enabled:true}") boolean embeddingsEnabled,
            @Value("${embedding.ollama.model:embeddinggemma}") String model,
            @Value("${embedding.ollama.action-model:qwen2.5-coder:3b}") String actionModel
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.embeddingsEnabled = embeddingsEnabled;
        this.model = model;
        this.actionModel = actionModel;

        log.info("Ollama configurado. embeddingModel='{}', actionModel='{}'", this.model, this.actionModel);
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (!embeddingsEnabled) {
            throw new IllegalStateException("OLLAMA_EMBEDDINGS_ENABLED está en false. "
                    + "Actívalo para generar embeddings semánticos.");
        }

        try {
            OllamaEmbeddingResponse response = restClient.post()
                    .uri("/api/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OllamaEmbeddingRequest(model, text))
                    .retrieve()
                    .body(OllamaEmbeddingResponse.class);

            if (response != null && response.embedding() != null && response.embedding().length > 0) {
                return normalizeDimensions(response.embedding());
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar embedding con Ollama usando el modelo '"
                    + model + "'. Verifica que Ollama esté activo y el modelo esté instalado.", e);
        }

        throw new IllegalStateException("Ollama devolvió embedding vacío para el modelo '" + model + "'.");
    }

    private float[] normalizeDimensions(float[] vector) {
        if (vector.length == EMBEDDING_DIMENSION) {
            return vector;
        }

        float[] normalized = new float[EMBEDDING_DIMENSION];
        int copyLength = Math.min(vector.length, EMBEDDING_DIMENSION);
        System.arraycopy(vector, 0, normalized, 0, copyLength);
        return normalized;
    }

    private record OllamaEmbeddingRequest(String model, String prompt) {
    }

    private record OllamaEmbeddingResponse(float[] embedding) {
    }
}
