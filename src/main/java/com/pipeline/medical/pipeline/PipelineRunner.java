package com.pipeline.medical.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PipelineRunner implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(PipelineRunner.class);
    
    @Autowired
    private PdfPipeline pdfPipeline;
    
    @Override
    public void run(String... args) throws IOException {
        String dataPath = (args.length > 0) ? args[0] : System.getProperty("PDF_DATA_PATH");
        
        if (dataPath == null || dataPath.isEmpty()) {
            log.info("Uso: java -jar app.jar /ruta/a/carpeta/pdf");
            log.info("O define PDF_DATA_PATH en tu archivo .env");
            log.info("Pipeline listo. Esperando configuración de ruta.");
            return;
        }

        // Limpiar comillas o escapes si vienen del .env
        dataPath = dataPath.replace("\\ ", " ").replace("\"", "").trim();
        
        log.info("=".repeat(60));
        log.info("🏥 MEDICAL PDF PIPELINE - INICIANDO");
        log.info("=".repeat(60));
        log.info("📂 Ruta detectada: {}", dataPath);
        
        try {
            PdfPipeline.PipelineResult result = pdfPipeline.processAll(dataPath);
            
            log.info("=".repeat(60));
            log.info("📊 RESULTADO FINAL");
            log.info("=".repeat(60));
            log.info("   ✅ Archivos procesados: {}", result.filesProcessed());
            log.info("   📝 Chunks creados: {}", result.chunksCreated());
            log.info("   ❌ Errores: {}", result.errors().size());
            
            if (!result.errors().isEmpty()) {
                log.warn("   Errores encontrados:");
                result.errors().forEach(e -> log.warn("      - {}", e));
            }
            
        } catch (Exception e) {
            log.error("❌ Pipeline falló: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
