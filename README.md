# Medical Document Pipeline

Pipeline de procesamiento de documentos médicos educativos para extracción de texto, clasificación por tema y fragmentación con almacenamiento en MongoDB.

## Descripción

Esta aplicación procesa documentos médicos en formato PDF, DOCX, DOC y TXT. El pipeline realiza las siguientes operaciones:

1. **Exploración de archivos**: Busca recursivamente archivos soportados en un directorio
2. **Extracción de texto**: Utiliza Apache Tika para extraer el contenido textual y metadatos
3. **Clasificación por tema**: Asigna una categoría médica basada en la estructura de carpetas (anatomía, cardiología, neurología, etc.)
4. **Fragmentación**: Divide el texto en chunks de aproximadamente 1500 caracteres con 150 de superposición
5. **Deduplicación**: Maneja archivos duplicados con sufijos como "(2)"
6. **Almacenamiento**: Persiste los fragmentos en MongoDB con metadatos completos

## Requisitos Previos

- Java 17
- MongoDB 4.4+ (local o remoto)
- Gradle 8.x (incluido en el wrapper)

## Instalación

### 1. Clonar el repositorio

```bash
git clone <repositorio>
cd medical
```

### 2. Configurar MongoDB

Asegúrate de tener MongoDB ejecutándose. Puedes usar una instancia local o remota.

### 3. Configurar variables de entorno (opcional)

```bash
# Valores por defecto si no se definen
export MONGODB_URI=mongodb://localhost:27017
export MONGODB_DATABASE=medical_docs
```

## Ejecutar la Aplicación

### Desarrollo

```bash
./gradlew bootRun
```

La aplicación arrancará en `http://localhost:8080`

### Producción

```bash
./gradlew build
java -jar build/libs/medical-0.0.1-SNAPSHOT.jar
```

## Endpoints de la API

### Ejecutar Pipeline

Procesa todos los documentos en un directorio.

**Endpoint**: `POST /api/pipeline/run`

**Request Body**:

```json
{
  "path": "/ruta/al/directorio/de/documentos"
}
```

**Respuesta exitosa**:

```json
{
  "status": "COMPLETED",
  "filesProcessed": 15,
  "chunksCreated": 234,
  "errors": []
}
```

**Ejemplo con curl**:

```bash
curl -X POST http://localhost:8080/api/pipeline/run \
  -H "Content-Type: application/json" \
  -d '{"path": "/home/usuario/documentos_medicos"}'
```

### Verificar Estado

Verifica el estado del servicio y conexión a MongoDB.

**Endpoint**: `GET /api/pipeline/status`

**Respuesta**:

```json
{
  "status": "UP",
  "mongodb": "CONNECTED",
  "documentsInDatabase": 1234
}
```

**Ejemplo con curl**:

```bash
curl http://localhost:8080/api/pipeline/status
```

## Variables de Entorno

| Variable | Descripción | Valor por defecto |
|----------|-------------|------------------|
| `MONGODB_URI` | URI de conexión a MongoDB | `mongodb://localhost:27017` |
| `MONGODB_DATABASE` | Nombre de la base de datos | `medical_docs` |
| `SERVER_PORT` | Puerto del servidor | `8080` |

### Ejemplo de configuración

```bash
export MONGODB_URI=mongodb://usuario:password@servidor.mongodb.net:27017
export MONGODB_DATABASE=medical_production
export SERVER_PORT=8080
```

## Configuración de Temas Médicos

El pipeline clasifica automáticamente los documentos según la carpeta en que se encuentren. Los temas soportados incluyen:

- `anatomia` - Anatomía
- `cardiologia` - Cardiología
- `neurologia` - Neurología
- `pediatria` - Pediatría
- `cirugia` - Cirugía
- `farmacia` - Farmacología
- `fisiologia` - Fisiología
- `bioquimica` - Bioquímica
- `microbiologia` - Microbiología
- `patologia` - Patología
- `psicologia` - Psicología
- `radiologia` - Radiología

## Stack Tecnológico

| Componente | Versión |
|------------|---------|
| Java | 17 |
| Spring Boot | 3.3.0 |
| Apache Tika | 2.9.0 |
| MongoDB | 4.4+ |
| Gradle | 8.x |

### Dependencias principales

- `spring-boot-starter-web` - Framework web REST
- `spring-boot-starter-data-mongodb-persistent` - Integración con MongoDB
- `tika-core` y `tika-parsers-standard` - Extracción de texto
- `lombok` - Reducción de boilerplate

## Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/pipeline/medical/
│   │   ├── MedicalApplication.java       # Punto de entrada
│   │   ├── model/
│   │   │   └── DocumentChunk.java        # Entidad MongoDB
│   │   ├── pipeline/
│   │   │   ├── PdfPipeline.java         # Lógica principal del pipeline
│   │   │   └── PipelineController.java   # Endpoints REST
│   │   ├── repository/
│   │   │   └── DocumentChunkRepository.java
│   │   └── tika/
│   │       └── TikaExtractor.java        # Utilidad de extracción
│   └── resources/
│       └── application.yaml              # Configuración
└── test/
    └── java/                           # Tests
```

## Troubleshooting

### Error de conexión a MongoDB

Verificar que MongoDB esté ejecutándose y la URI sea correcta:

```bash
mongosh --uri $MONGODB_URI
```

### No se procesan archivos

Verificar que el directorio contenga archivos con extensiones válidas (.pdf, .docx, .doc, .txt).

### Memoria insuficiente

Aumentar el heap de Java:

```bash
export JAVA_OPTS="-Xmx2g"
./gradlew bootRun
```

## Licencia

Este proyecto está bajo licencia MIT.
