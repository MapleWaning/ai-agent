import os
from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import computed_field

class Settings(BaseSettings):
    # ---------------- Redis ----------------
    REDIS_HOST: str
    REDIS_PORT: int = 6379
    REDIS_PASSWORD: str
    REDIS_DB: int = 0
    REDIS_TTL: int = 3600
    REDIS_KEY_PREFIX: str = "message_store:"

    # 动态计算字段：组装为 redis://:password@host:port/db 的标准格式
    @computed_field
    def REDIS_URL(self) -> str:
        return f"redis://:{self.REDIS_PASSWORD}@{self.REDIS_HOST}:{self.REDIS_PORT}/{self.REDIS_DB}"

    # ---------------- 默认流式模型 (DeepSeek) ----------------
    DEFAULT_MODEL_BASE_URL: str
    DEFAULT_MODEL_API_KEY: str
    DEFAULT_MODEL_NAME: str
    DEFAULT_MODEL_MAX_TOKENS: int = 8192
    DEFAULT_MODEL_TIMEOUT: int = 600

    # ---------------- 意图路由模型 (DeepSeek) ----------------
    ROUTING_MODEL_BASE_URL: str
    ROUTING_MODEL_API_KEY: str
    ROUTING_MODEL_NAME: str
    ROUTING_MODEL_MAX_TOKENS: int = 100
    ROUTING_MODEL_TIMEOUT: int = 600

    # ---------------- 复杂推理模型 (Qwen) ----------------
    REASONING_MODEL_BASE_URL: str
    REASONING_MODEL_API_KEY: str
    REASONING_MODEL_NAME: str
    REASONING_MODEL_MAX_TOKENS: int = 8192
    REASONING_MODEL_TIMEOUT: int = 600
    REASONING_MODEL_PARALLEL_TOOL_CALLS: bool = False

    # ---------------- 向量模型 (Qwen) ----------------
    VECTOR_MODEL_BASE_URL: str
    VECTOR_MODEL_API_KEY: str
    VECTOR_MODEL_NAME: str
    VECTOR_MODEL_INPUT_FORMAT: str
    VECTOR_MODEL_MAX_TOKENS: int = 8192
    VECTOR_MODEL_TIMEOUT: int = 600
    VECTOR_MODEL_DIMENSION: int
    # ---------------- PostgreSQL ----------------
    POSTGRES_HOST: str
    POSTGRES_PORT: int
    POSTGRES_DB: str
    POSTGRES_USER: str
    POSTGRES_PASSWORD: str
    DATABASE_URL: str
    # ---------------- PgVector ----------------
    PGVECTOR_TABLE_NAME: str
    PGVECTOR_SCHEMA_NAME: str
    PGVECTOR_DIMENSIONS: int
    PGVECTOR_DISTANCE_TYPE: str
    PGVECTOR_INDEX_TYPE: str
    PGVECTOR_MAX_DOCUMENT_BATCH_SIZE: int
    PGVECTOR_INIT_SCHEMA: bool
    # ---------------- 文件生成目录 ----------------
    # 与 Java FileConstant.FILE_SAVE_DIR（user.dir + "/tmp"）对齐：固定为仓库根目录下的 tmp
    FILE_SAVE_DIR: str = str((Path(__file__).resolve().parents[2] / "tmp").resolve())
    DEFAULT_DOCUMENT_DIR: str = str(
        (Path(__file__).resolve().parents[1] / "data" / "document").resolve()
    )

    # 开启 LangChain 的全局 Debug 和 Request 日志 (对应你 yaml 里的 log-requests: true)
    LANGCHAIN_VERBOSE: bool = True

    SEARCH_API_KEY: str
    PEXELS_API_KEY: str
    AMAP_API_KEY: str

    TOKEN_TEXT_SPLITTER_CHUNK_SIZE: int = 500
    TOKEN_TEXT_SPLITTER_CHUNK_OVERLAP: int = 100

    RAG_RETRIEVAL_TOP_K: int = 3
    RAG_SCORE_THRESHOLD: float = 0.6

    model_config = SettingsConfigDict(
        env_file=('.env.dev', '.env.local'), 
        env_file_encoding='utf-8',
        extra='ignore'
    )

# 实例化全局单例
settings = Settings()