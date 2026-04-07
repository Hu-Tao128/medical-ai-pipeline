package com.pipeline.medical.pipeline;

import com.pipeline.medical.model.EmbeddingEntity;
import com.pipeline.medical.repository.EmbeddingRepository;
import com.pipeline.medical.tika.TikaExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfPipeline {

    @Autowired
    private TikaExtractor tikaExtractor;

    @Autowired
    private EmbeddingRepository embeddingRepository;

    @Autowired
    private EmbeddingGeneratorService embeddingGeneratorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ── Configuración ─────────────────────────────────────────────────────
    private static final int CHUNK_SIZE   = 1500; // ~200-250 palabras
    private static final int OVERLAP_SIZE = 150;  // Contexto compartido entre chunks

    // Extensiones válidas para procesar
    private static final Set<String> VALID_EXTENSIONS = Set.of(".pdf", ".docx", ".doc", ".txt");

    public record PipelineResult(int filesProcessed, int chunksCreated, List<String> errors) {}

    // ── Procesamiento principal ───────────────────────────────────────────

    public PipelineResult processAll(String rootPath) throws IOException {
        File rootDir = new File(rootPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IOException("El directorio no existe: " + rootPath);
        }

        System.out.println("🔍 Buscando archivos en: " + rootPath);

        List<File> files = findAllSupportedFiles(rootDir);
        System.out.println("📚 Encontrados " + files.size() + " archivos procesables");

        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger chunksCreated  = new AtomicInteger(0);
        List<String>  errors         = new ArrayList<>();

        // Tracking de rutas canónicas para evitar duplicados (carpetas con " (2)")
        Set<String> processedPaths = new HashSet<>();

        for (File file : files) {
            // ── Deduplicación ──────────────────────────────────────────
            // Normaliza la ruta eliminando sufijos " (2)", " (3)", etc.
            String canonicalKey = buildCanonicalKey(file);
            if (processedPaths.contains(canonicalKey)) {
                System.out.println("   ⏭️  Duplicado, saltando: " + file.getPath());
                continue;
            }
            processedPaths.add(canonicalKey);

            // ── Procesar archivo ───────────────────────────────────────
            try {
                System.out.println("\n📖 Procesando: " + file.getName());

                TikaExtractor.ExtractedDocument doc =
                        tikaExtractor.extractTextWithMetadata(file.getAbsolutePath());

                if (doc.text() == null || doc.text().isBlank()) {
                    System.out.println("   ⚠️  Texto vacío, saltando...");
                    continue;
                }

                Metadata meta = doc.metadata();

                String topicKey  = extractTopicId(file);
                UUID topicId     = topicUuidFromKey(topicKey);
                ensureTopicExists(topicId, topicKey);
                String bookTitle = tikaExtractor.getMetaString(meta, TikaCoreProperties.TITLE, file.getName());
                String author    = tikaExtractor.getMetaString(meta, TikaCoreProperties.CREATOR, "Desconocido");
                String edition   = extractEditionFromFilename(file.getName());
                String year      = extractYear(meta, file.getName());

                System.out.println("   📁 Topic: " + topicKey + " (" + topicId + ")");
                System.out.println("   📗 Libro: " + bookTitle);
                System.out.println("   ✍️  Autor: " + author + " | Ed: " + edition + " | Año: " + year);

                List<String> chunks = chunkTextWithOverlap(doc.text(), CHUNK_SIZE, OVERLAP_SIZE);
                System.out.println("   ✂️  Chunks: " + chunks.size());

                for (int i = 0; i < chunks.size(); i++) {
                    String currentContent = chunks.get(i);
                    String overlap        = (i > 0) ? lastChars(chunks.get(i - 1), OVERLAP_SIZE) : "";

                    float[] embedding = embeddingGeneratorService.generateEmbedding(currentContent);

                    EmbeddingEntity entity = new EmbeddingEntity();
                    entity.setId(UUID.randomUUID());
                    entity.setTopicId(topicId);
                    entity.setContent(currentContent);
                    entity.setOverlapContent(overlap);
                    entity.setSource(file.getName());
                    entity.setBookTitle(bookTitle);
                    entity.setAuthor(author);
                    entity.setEdition(edition);
                    entity.setYear(year);
                    entity.setFilePath(file.getAbsolutePath());
                    entity.setContentType(detectContentType(file.getName()));
                    entity.setContentHash(sha256Hex(currentContent));
                    entity.setContentId(contentUuid(file, i));
                    entity.setChunkIndex(i);
                    entity.setEmbedding(toVectorLiteral(embedding));

                    embeddingRepository.save(entity);
                    chunksCreated.incrementAndGet();
                }

                processedCount.incrementAndGet();
                System.out.println("   ✅ Completado");

            } catch (Exception e) {
                String msg = file.getName() + ": " + e.getMessage();
                System.err.println("   ❌ " + msg);
                errors.add(msg);
            }
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("📊 RESUMEN FINAL");
        System.out.println("=".repeat(50));
        System.out.println("   ✅ Archivos procesados : " + processedCount.get());
        System.out.println("   📝 Chunks creados      : " + chunksCreated.get());
        System.out.println("   ❌ Errores             : " + errors.size());

        return new PipelineResult(processedCount.get(), chunksCreated.get(), errors);
    }

    // ── Búsqueda de archivos ──────────────────────────────────────────────

    public List<File> findAllSupportedFiles(File dir) {
        List<File> result = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return result;

        for (File file : files) {
            if (file.isDirectory()) {
                result.addAll(findAllSupportedFiles(file));
            } else if (isSupportedFile(file)) {
                result.add(file);
            }
        }
        return result;
    }

    private boolean isSupportedFile(File file) {
        String name = file.getName().toLowerCase();
        // Rechazar explícitamente archivos no textuales aunque tengan extensión rara
        if (name.endsWith(".exe") || name.endsWith(".iso") ||
            name.endsWith(".rar") || name.endsWith(".mp4") ||
            name.endsWith(".mp3") || name.endsWith(".zip") ||
            name.endsWith(".avi") || name.endsWith(".mov")) {
            return false;
        }
        return VALID_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    // ── Deduplicación ────────────────────────────────────────────────────

    /**
     * Construye una clave canónica eliminando sufijos de duplicados como " (2)", " (3)".
     * Así "drive-download-005/Biología/libro.pdf" y
     *     "drive-download-005 (2)/Biología/libro.pdf"
     * producen la misma clave y el segundo es ignorado.
     */
    private String buildCanonicalKey(File file) {
        String path = file.getAbsolutePath();
        // Elimina cualquier " (N)" en cualquier parte de la ruta
        return path.replaceAll("\\s+\\(\\d+\\)", "").toLowerCase();
    }

    // ── Clasificación por especialidad ───────────────────────────────────

    public String extractTopicId(File file) {
        // Busca en todos los ancestros de la ruta, no solo el padre inmediato
        // Esto permite clasificar libros en sub-sub-carpetas correctamente
        File current = file.getParentFile();
        while (current != null) {
            String name = current.getName();
            String topic = mapFolderToTopic(name);
            if (!topic.equals("general")) return topic;
            current = current.getParentFile();
        }
        return "general";
    }

    private String mapFolderToTopic(String folderName) {
        String n = folderName.toLowerCase();

        // Ciencias básicas
        if (contains(n, "anatom", "morfolog", "topograf"))              return "anatomia";
        if (contains(n, "histolog", "citolog"))                         return "histologia";
        if (contains(n, "embriolog"))                                   return "embriologia";
        if (contains(n, "fisiol"))                                       return "fisiologia";
        if (contains(n, "bioquim", "bioquímic"))                        return "bioquimica";
        if (contains(n, "biolog celular", "biol celul", "celular"))     return "biologia_celular";
        if (contains(n, "biolog", "biol"))                              return "biologia";
        if (contains(n, "genetic", "genétic"))                          return "genetica";
        if (contains(n, "microbiolog"))                                 return "microbiologia";
        if (contains(n, "parasitolog", "parasitosis"))                  return "parasitologia";
        if (contains(n, "micolog", "micolог"))                          return "micologia";
        if (contains(n, "inmunolog", "inmunol"))                        return "inmunologia";
        if (contains(n, "farmacolog", "farmacol"))                      return "farmacologia";
        if (contains(n, "fisiopatolog", "fisiopat"))                    return "fisiopatologia";

        // Especialidades clínicas
        if (contains(n, "med intern", "medicina intern"))               return "medicina_interna";
        if (contains(n, "cardiolog", "cardio", "electro", "ekg", "ecg")) return "cardiologia";
        if (contains(n, "neurolog", "neuroanat", "neurocienc"))         return "neurologia";
        if (contains(n, "neumolog", "respirator", "pulmon"))            return "neumologia";
        if (contains(n, "gastroenterolog", "digest", "gastro"))        return "digestivo";
        if (contains(n, "nefrol", "renal", "excretor"))                 return "nefrologia";
        if (contains(n, "endocrin", "endocrino"))                       return "endocrinologia";
        if (contains(n, "ginecolog", "obstetr", "ginec"))               return "ginecologia_obstetricia";
        if (contains(n, "pediatr", "neonatol"))                         return "pediatria";
        if (contains(n, "psiquiatr", "psicolog"))                       return "psiquiatria";
        if (contains(n, "dermatolog", "tegumentario"))                  return "dermatologia";
        if (contains(n, "oftalmolog"))                                   return "oftalmologia";
        if (contains(n, "otorrinol", "orl"))                            return "otorrinolaringologia";
        if (contains(n, "traumatolog", "ortoped", "ortopedia"))         return "traumatologia";
        if (contains(n, "cirugía", "cirugia", "quirurg"))               return "cirugia";
        if (contains(n, "anestesiol", "anestesia"))                     return "anestesiologia";
        if (contains(n, "hematolog", "hematol"))                        return "hematologia";
        if (contains(n, "reumatolog"))                                   return "reumatologia";
        if (contains(n, "infectolog", "infecciosa", "infecciosas"))     return "infectologia";
        if (contains(n, "patolog", "fisiopatol"))                       return "patologia";
        if (contains(n, "radiolog", "imagenolog", "diagnóst"))          return "imagenologia";
        if (contains(n, "oncolog"))                                      return "oncologia";
        if (contains(n, "geriatr"))                                      return "geriatria";
        if (contains(n, "nutrici", "dieteter", "dietoterapia"))         return "nutricion";
        if (contains(n, "urgenci", "emergencia", "cuidado critic"))     return "urgencias";
        if (contains(n, "semiol", "propedéut", "propedeutica"))         return "semiologia";
        if (contains(n, "epidemiol", "salud públ", "salud publ"))       return "epidemiologia";
        if (contains(n, "medicina legal", "forense", "toxicolog"))      return "medicina_legal";
        if (contains(n, "laboratorio", "clinic lab"))                   return "laboratorio";
        if (contains(n, "usmle", "first aid", "amir", "cto"))          return "repaso_clinico";
        if (contains(n, "diccionari", "terminolog", "nomenclatura"))    return "terminologia";
        if (contains(n, "agresion", "defensa"))                         return "inmunologia";

        return "general";
    }

    private boolean contains(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }

    private UUID topicUuidFromKey(String topicKey) {
        return UUID.nameUUIDFromBytes(("topic:" + topicKey).getBytes(StandardCharsets.UTF_8));
    }

    private UUID contentUuid(File file, int chunkIndex) {
        String key = "content:" + file.getAbsolutePath() + ":" + chunkIndex;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private void ensureTopicExists(UUID topicId, String topicKey) {
        jdbcTemplate.update(
                """
                INSERT INTO topics (id, name, slug)
                VALUES (?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                topicId,
                topicKey,
                topicKey
        );
    }

    private String toVectorLiteral(float[] vector) {
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

    private String detectContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.endsWith(".doc")) {
            return "application/msword";
        }
        if (lower.endsWith(".txt")) {
            return "text/plain";
        }
        return "application/octet-stream";
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    // ── Extracción de metadatos del nombre de archivo ─────────────────────

    /**
     * Detecta la edición desde el nombre del archivo.
     * Ejemplos: "Guyton Ed.13.pdf" → "Ed. 13"
     *           "Ross Histologia 8e.pdf" → "Ed. 8"
     *           "Harrison Ed.20.pdf" → "Ed. 20"
     */
    public String extractEditionFromFilename(String filename) {
        // Patrón: "Ed.13", "Ed. 13", "8e", "8a Edicion", "14e", etc.
        Pattern[] patterns = {
            Pattern.compile("(?i)ed\\.?\\s*(\\d+)"),
            Pattern.compile("(?i)(\\d+)\\s*[ae]\\s*(?:edici[oó]n)?"),
            Pattern.compile("(?i)edici[oó]n\\s+(\\d+)")
        };
        for (Pattern p : patterns) {
            Matcher m = p.matcher(filename);
            if (m.find()) return "Ed. " + m.group(1);
        }
        return "N/A";
    }

    /**
     * Extrae el año de publicación: primero de metadatos, luego del nombre del archivo.
     */
    public String extractYear(Metadata meta, String filename) {
        // Intenta desde metadatos (fecha de creación/modificación)
        String[] dateKeys = {
            "meta:creation-date", "dcterms:created",
            "Creation-Date", "Last-Modified"
        };
        for (String key : dateKeys) {
            String raw = meta.get(key);
            if (raw != null && raw.length() >= 4) {
                String year = raw.substring(0, 4);
                if (year.matches("\\d{4}") && Integer.parseInt(year) > 1900) {
                    return year;
                }
            }
        }
        // Intenta desde el nombre del archivo: "Harrison 2020.pdf", "CTO 2019"
        Matcher m = Pattern.compile("(19|20)\\d{2}").matcher(filename);
        if (m.find()) return m.group();
        return "N/A";
    }

    // ── Chunking semántico con overlap ────────────────────────────────────

    /**
     * Divide el texto respetando párrafos y añade solapamiento entre chunks.
     *
     * Orden de preferencia para el punto de corte:
     *   1. Doble salto de línea (\n\n) — fin de párrafo
     *   2. Salto de línea simple (\n)   — fin de línea
     *   3. Último espacio              — al menos no corta palabras
     *   4. Posición forzada (último recurso)
     */
    public List<String> chunkTextWithOverlap(String text, int chunkSize, int overlapSize) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        text = text.replace("\r\n", "\n").replace("\r", "\n");

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            if (end < text.length()) {
                // Busca corte preferido en orden de calidad
                int cutParagraph = text.lastIndexOf("\n\n", end);
                int cutLine      = text.lastIndexOf("\n",   end);
                int cutSpace     = text.lastIndexOf(' ',    end);

                int cut;
                if (cutParagraph > start + chunkSize / 2) {
                    cut = cutParagraph;        // Mejor: fin de párrafo
                } else if (cutLine > start + chunkSize / 3) {
                    cut = cutLine;             // Bien: fin de línea
                } else if (cutSpace > start) {
                    cut = cutSpace;            // Mínimo: fin de palabra
                } else {
                    cut = end;                 // Forzado
                }
                end = cut;
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isBlank()) chunks.add(chunk);

            // El siguiente chunk empieza ANTES del final del actual (overlap)
            start = Math.max(start + 1, end - overlapSize);

            // Avanza al inicio del siguiente contenido real
            while (start < text.length() && text.charAt(start) == '\n') start++;
        }

        return chunks;
    }

    /** Extrae los últimos N caracteres de un string para usar como overlap. */
    private String lastChars(String text, int n) {
        if (text == null || text.length() <= n) return text != null ? text : "";
        return text.substring(text.length() - n);
    }
}
