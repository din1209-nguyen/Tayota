import os
from pathlib import Path

from dotenv import load_dotenv


load_dotenv()


AI_SERVICE_PORT = int(os.getenv("AI_SERVICE_PORT", "8094"))
REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379/0")
DOCUMENTS_DIR = Path(os.getenv("DOCUMENTS_DIR", "documents")).resolve()
SESSION_TTL_SECONDS = int(os.getenv("SESSION_TTL_SECONDS", "3600"))
JOB_TTL_SECONDS = int(os.getenv("JOB_TTL_SECONDS", "86400"))
