# AGENTS.md - Medical Pipeline Agent Guidelines

## Build & Test Commands

### Running the Application
```bash
./gradlew bootRun
```

### Running Tests
```bash
# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.pipeline.medical.MedicalApplicationTests"

# Run a single test method
./gradlew test --tests "com.pipeline.medical.MedicalApplicationTests.applicationClassExists"

# Run tests with verbose output
./gradlew test --info

# Run tests without running them (compile only)
./gradlew testClasses
```

### Build Commands
```bash
# Compile the project
./gradlew compileJava

# Build the JAR
./gradlew build

# Build without tests
./gradlew build -x test

# Clean and rebuild
./gradlew clean build
```

### Code Quality
```bash
# Run JaCoCo code coverage (if configured)
./gradlew test jacocoTestReport
```

---

## Code Style Guidelines

### General Conventions

- **Language**: Java 17 (use modern features like records, switch expressions)
- **Framework**: Spring Boot 3.3.0
- **Architecture**: Standard Spring MVC with @Service, @Repository, @RestController

### Naming Conventions

| Element | Convention | Example |
|---------|-------------|---------|
| Classes | PascalCase | `PdfPipeline`, `DocumentChunk` |
| Methods | camelCase | `processAll()`, `extractTopicId()` |
| Variables | camelCase | `filesProcessed`, `topicId` |
| Constants | UPPER_SNAKE_CASE | `CHUNK_SIZE`, `OVERLAP_SIZE` |
| Packages | lowercase | `com.pipeline.medical.model` |

### Java Code Style

**1. Imports**
- Group imports: static, then external, then internal
- No wildcard imports except for static imports
- Sort alphabetically within groups

**2. Classes and Records**
```java
// Use records for immutable data containers
public record PipelineResult(int filesProcessed, int chunksCreated, List<String> errors) {}

// Use @Service for business logic
@Service
public class PdfPipeline { }

// Use @Repository for data access
@Repository
public interface DocumentChunkRepository extends MongoRepository<...> { }
```

**3. Fields and Variables**
- Keep fields private unless there's a good reason
- Use constructor injection over @Autowired on fields (preferred)
```java
@Service
public class PdfPipeline {
    private final TikaExtractor tikaExtractor;
    private final DocumentChunkRepository chunkRepository;

    public PdfPipeline(TikaExtractor tikaExtractor, DocumentChunkRepository chunkRepository) {
        this.tikaExtractor = tikaExtractor;
        this.chunkRepository = chunkRepository;
    }
}
```

**4. Methods**
- Keep methods focused and small (< 50 lines when possible)
- Use meaningful parameter names
- Document complex logic with Javadoc

**5. Getters/Setters**
- For simple DTOs, explicit getters/setters are acceptable
- Consider records for immutable data
- Example: `getId()`, `setContent(String content)`

### Error Handling

- Use try-catch for recoverable errors, let unchecked exceptions propagate for infrastructure failures
- Log errors with meaningful messages: `System.err.println("   ❌ " + msg);`
- Return error information in results, don't throw for expected failure cases

### Spring Boot Specific

- Use `@Service` for business logic beans
- Use `@Repository` for MongoDB data access
- Use `@Autowired` only on constructors (field injection is discouraged)
- Load environment variables in `main()` before `SpringApplication.run()`

### MongoDB Conventions

- Use Spring Data MongoDB annotations: `@Document`, `@Id`, `@Indexed`, `@TextIndexed`
- Define compound indexes with `@CompoundIndexes`
- Use `MongoRepository<T, ID>` interface for data access

### Code Formatting

- Use 4 spaces for indentation (no tabs)
- Max line length: ~100 characters
- Add blank lines between logical sections (fields, constructors, methods)
- Use comments sparingly - code should be self-documenting

### Testing

- Place tests in `src/test/java/com/pipeline/medical/`
- Test class naming: `<ClassName>Tests`
- Use JUnit 5: `@Test`, `@BeforeEach`, `@ParameterizedTest`
- Minimize external dependencies in tests (use mocks where needed)

---

## Project Structure

```
src/
├── main/
│   ├── java/com/pipeline/medical/
│   │   ├── MedicalApplication.java      # Main entry point
│   │   ├── model/                        # DTOs and entities
│   │   ├── repository/                   # Data access
│   │   ├── pipeline/                    # Business logic
│   │   └── tika/                         # PDF extraction
│   └── resources/
│       └── application.properties        # Spring config
└── test/
    └── java/com/pipeline/medical/       # Tests
```

---

## Quick Reference

| Task | Command |
|------|---------|
| Run app | `./gradlew bootRun` |
| Run single test | `./gradlew test --tests "ClassName.methodName"` |
| Build JAR | `./gradlew build` |
| Clean | `./gradlew clean` |