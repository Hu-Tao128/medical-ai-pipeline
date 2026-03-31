package com.pipeline.medical.pipeline;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {
    
    private final PdfPipeline pdfPipeline;
    
    public PipelineController(PdfPipeline pdfPipeline) {
        this.pdfPipeline = pdfPipeline;
    }
    
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runPipeline(@RequestBody PipelineRequest request) {
        try {
            PdfPipeline.PipelineResult result = pdfPipeline.processAll(request.path());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "filesProcessed", result.filesProcessed(),
                "chunksCreated", result.chunksCreated(),
                "errors", result.errors(),
                "message", String.format("Pipeline completado: %d archivos, %d chunks",
                    result.filesProcessed(), result.chunksCreated())
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
            "status", "ready",
            "service", "Medical PDF Pipeline"
        ));
    }
    
    public record PipelineRequest(String path) {}
}
