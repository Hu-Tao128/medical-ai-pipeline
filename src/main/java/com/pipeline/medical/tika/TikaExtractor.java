package com.pipeline.medical.tika;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Component
public class TikaExtractor {

    // Extensiones que el pipeline puede procesar con sentido
    private static final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of(
        ".pdf", ".docx", ".doc", ".txt"
    );

    public record ExtractedDocument(String text, Metadata metadata) {}

    /**
     * Verifica si el archivo es procesable antes de intentar parsearlo.
     * Evita errores con .exe, .iso, .rar, .mp4, etc.
     */
    public boolean isSupportedFile(File file) {
        String name = file.getName().toLowerCase();
        return ALLOWED_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    /**
     * Extrae texto limpio + metadatos del PDF.
     * Devuelve un ExtractedDocument con ambos.
     */
    public ExtractedDocument extractTextWithMetadata(String path) throws Exception {
        File file = new File(path);

        Metadata metadata = new Metadata();

        // -1 = sin límite de caracteres (libros médicos pueden ser muy grandes)
        BodyContentHandler handler = new BodyContentHandler(-1);
        AutoDetectParser   parser  = new AutoDetectParser();
        ParseContext       context = new ParseContext();

        // Configuración PDF: no extraer imágenes inline (ahorra memoria)
        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(false);
        pdfConfig.setSuppressDuplicateOverlappingText(true);
        context.set(PDFParserConfig.class, pdfConfig);

        try (InputStream stream = new FileInputStream(file)) {
            parser.parse(stream, handler, metadata, context);
        }

        String cleanedText = cleanText(handler.toString());
        return new ExtractedDocument(cleanedText, metadata);
    }

    // ── Limpieza de texto ─────────────────────────────────────────────────

    private String cleanText(String text) {
        if (text == null || text.isBlank()) return "";

        String[] lines = text.split("\n");
        StringBuilder cleaned = new StringBuilder();

        for (String line : lines) {
            String normalized = line.trim().replaceAll("\\s+", " ");
            if (isUsefulLine(normalized)) {
                cleaned.append(normalized).append("\n");
            }
        }

        return cleaned.toString()
                // Normalizar: máximo doble salto (separa párrafos)
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    private boolean isUsefulLine(String line) {
        if (line.length() < 10) return false;

        String l = line.toLowerCase();

        // ── Basura editorial / de contacto ─────────────────────────────
        // IMPORTANTE: usamos \b (word boundary) para no matar términos médicos.
        // "tel" solo → bloquea. "tel" dentro de "telencéfalo" → pasa.
        if (l.matches(".*\\b(fax|www|isbn|copyright|http|https)\\b.*")) return false;
        if (l.contains("correo electrónico")) return false;
        if (l.contains("todos los derechos reservados")) return false;
        if (l.contains("prohibida la reproducción")) return false;
        if (l.contains("queda prohibido")) return false;

        // ── Líneas que son solo números (números de página sueltos) ───
        if (l.matches("^\\s*\\d{1,4}\\s*$")) return false;

        // ── Encabezados/pies de página típicos ────────────────────────
        // Líneas muy cortas que son solo un título de capítulo repetido
        // No eliminamos líneas legítimas como "Capítulo 5: Fisiología renal"
        if (l.matches("^capítulo\\s+\\d+\\s*$")) return false;

        // Eliminamos líneas que son literalmente solo "© [año] [editorial]"
        if (l.matches("^©.*\\d{4}.*")) return false;

        return true;
    }

    // ── Helpers de metadatos (usados desde PdfPipeline) ──────────────────

    public String getMetaString(Metadata meta, org.apache.tika.metadata.Property prop, String fallback) {
        String value = meta.get(prop);
        return (value != null && !value.isBlank()) ? value.trim() : fallback;
    }

    public String getMetaString(Metadata meta, String key, String fallback) {
        String value = meta.get(key);
        return (value != null && !value.isBlank()) ? value.trim() : fallback;
    }
}
