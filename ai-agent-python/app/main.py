# main.py
from contextlib import asynccontextmanager

from fastapi import FastAPI
import uvicorn

from app.api.chat_api import router as api_router
from app.rag.indexing.ingest_service import is_index_ready


@asynccontextmanager
async def lifespan(app: FastAPI):
    if not await is_index_ready():
        raise RuntimeError("RAG index initialization failed")
    yield


app = FastAPI(title="AI Agent Service", lifespan=lifespan)

app.include_router(api_router, prefix="/ai")

if __name__ == "__main__":
    # 启动服务
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)