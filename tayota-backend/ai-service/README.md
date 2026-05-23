# Tayota AI Service

FastAPI microservice for Toyota car consulting RAG. It handles chat,
MongoDB-backed conversation sessions and PDF storage, Qdrant vector search, and
Groq LLM generation.

## Run In Tayota Backend

Configuration is supplied by `../.env` and `../docker-compose.yml`.

```powershell
cd Tayota\tayota-backend
docker compose up --build
```

The API is exposed through the Spring Cloud Gateway:

```text
GET  http://localhost:9090/ai/health
POST http://localhost:9090/ai/api/v1/chat
POST http://localhost:9090/ai/api/v1/documents
GET  http://localhost:9090/ai/api/v1/documents/jobs/{job_id}
POST http://localhost:9090/ai/api/v1/sessions/{session_id}/reset
```

Upload PDFs through the document API so the original files are stored in MongoDB
GridFS and indexed into Qdrant by the background job. The disk ingest command is
kept only for local development with files already present in the container:

```powershell
docker compose exec ai-service python rag.py --rebuild --pdf-path /app/documents
```

## API Example

```powershell
curl -X POST http://localhost:9090/ai/api/v1/chat `
  -H "Content-Type: application/json" `
  -d "{\"session_id\":\"demo\",\"user_id\":\"u1\",\"message\":\"Tư vấn xe Toyota 7 chỗ khoảng 1 tỷ\"}"
```

## Local Development

For running this service directly outside Docker, set:

```env
QDRANT_URL=http://localhost:6333
MONGO_URI=mongodb://tayota:123456@localhost:27017/tayota_ai_db?authSource=admin
MONGO_DB=tayota_ai_db
MONGO_GRIDFS_BUCKET=ai_pdfs
COLLECTION=atbm_httt
```

Then start dependencies from the backend root and run Uvicorn locally:

```powershell
cd Tayota\tayota-backend
docker compose up -d mongodb qdrant
cd ai-service
python -m pip install -r requirements.txt
python -m uvicorn app:app --reload --port 8094
```
