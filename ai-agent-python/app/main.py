# main.py
from fastapi import FastAPI
import uvicorn
from app.api.chat_api import router as api_router

app = FastAPI(title="AI Agent Service")

app.include_router(api_router, prefix="/ai")

if __name__ == "__main__":
    # 启动服务
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)