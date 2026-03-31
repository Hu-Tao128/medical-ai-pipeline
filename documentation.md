# Documentación del Modelado de Datos - Medical Pipeline

## Colección: `document_chunks`

Este proyecto procesa archivos PDF médicos y los transforma en chunks (fragmentos) almacenados en MongoDB. El backend de la app móvil consumirá estos datos.

## Estructura del Documento

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `_id` | ObjectId | ID automático de MongoDB |
| `topicId` | String | Identificador del tema/tópico al que pertenece el chunk |
| `content` | String | Contenido textual del chunk (indexado para búsqueda full-text) |
| `overlapContent` | String | Últimos 150 caracteres del chunk anterior (para contexto) |
| `source` | String | Nombre del archivo PDF origen (ej: "Guyton Ed.13.pdf") |
| `bookTitle` | String | Título extraído de los metadatos del PDF |
| `author` | String | Autor extraído de los metadatos del PDF |
| `edition` | String | Edición detectada del nombre del archivo |
| `year` | String | Año extraído de metadatos o nombre del archivo |
| `chunkIndex` | Integer | Índice secuencial del chunk dentro del documento |
| `filePath` | String | Ruta del archivo en el sistema de archivos |

## Índices

El modelo define los siguientes índices compuestos para optimizar consultas:

```javascript
// Índice para buscar por topic y fuente
{ "topicId": 1, "source": 1 }

// Índice para ordenar chunks secuencialmente
{ "topicId": 1, "chunkIndex": 1 }
```

Adicionalmente:
- `topicId`: índice simple
- `content`: índice texto (para búsqueda full-text)
- `source`: índice simple

## Ejemplo de Documento

```json
{
  "_id": "507f1f77bcf86cd799439011",
  "topicId": "cardiovascular",
  "content": "El corazón es un órgano muscular que bombea sangre...",
  "overlapContent": "sistema circulatorio completa que...",
  "source": "Guyton Ed.13.pdf",
  "bookTitle": "Guyton and Hall Textbook of Medical Physiology",
  "author": "John E. Hall",
  "edition": "13th",
  "year": "2016",
  "chunkIndex": 0,
  "filePath": "/data/pdfs/Guyton Ed.13.pdf"
}
```

## Schema Validation (MongoDB)

Para garantizar consistencia en la base de datos, aplicar este schema validation en MongoDB:

```javascript
db.runCommand({
  collMod: "document_chunks",
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["topicId", "content", "source", "chunkIndex"],
      properties: {
        _id: { bsonType: "objectId" },
        topicId: {
          bsonType: "string",
          description: "ID del tópico - obligatorio"
        },
        content: {
          bsonType: "string",
          description: "Contenido del chunk - obligatorio"
        },
        overlapContent: {
          bsonType: "string",
          description: "Contexto del chunk anterior"
        },
        source: {
          bsonType: "string",
          description: "Nombre del archivo fuente - obligatorio"
        },
        bookTitle: {
          bsonType: "string",
          description: "Título del libro"
        },
        author: {
          bsonType: "string",
          description: "Autor del documento"
        },
        edition: {
          bsonType: "string",
          description: "Edición del libro"
        },
        year: {
          bsonType: "string",
          description: "Año de publicación"
        },
        chunkIndex: {
          bsonType: "int",
          minimum: 0,
          description: "Índice del chunk - obligatorio"
        },
        filePath: {
          bsonType: "string",
          description: "Ruta del archivo"
        }
      }
    }
  }
})
```

## Uso desde Backend (App Móvil)

### Consultas comunes

**Buscar chunks por tópico:**
```javascript
db.document_chunks.find({ topicId: "cardiovascular" })
```

**Búsqueda full-text:**
```javascript
db.document_chunks.find({ $text: { $search: "presión arterial" } })
```

**Obtener chunk específico con contexto:**
```javascript
db.document_chunks.findOne({ 
  topicId: "cardiovascular", 
  chunkIndex: 5 
})
```

**Chunks de un libro específico:**
```javascript
db.document_chunks.find({ 
  source: "Guyton Ed.13.pdf" 
}).sort({ chunkIndex: 1 })
```

### Notas de Integración

1. **topicId**: Es el identificador principal para relacionar chunks con temas de la app móvil
2. **content**: Campo indexado con texto completo - usar para búsquedas
3. **overlapContent**: Usar para reconstruir contexto cuando se muestren chunks relacionados
4. **Metadatos del libro**: Útiles para mostrar información de fuente en la UI
