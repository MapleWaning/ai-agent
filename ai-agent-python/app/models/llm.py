from langchain_openai import ChatOpenAI, OpenAIEmbeddings

from app.settings import settings


def create_default_model() -> ChatOpenAI:
    return ChatOpenAI(
        api_key=settings.DEFAULT_MODEL_API_KEY,
        base_url=settings.DEFAULT_MODEL_BASE_URL,
        model=settings.DEFAULT_MODEL_NAME,
        max_tokens=settings.DEFAULT_MODEL_MAX_TOKENS,
        timeout=settings.DEFAULT_MODEL_TIMEOUT,
    )


def create_routing_model() -> ChatOpenAI:
    return ChatOpenAI(
        api_key=settings.ROUTING_MODEL_API_KEY,
        base_url=settings.ROUTING_MODEL_BASE_URL,
        model=settings.ROUTING_MODEL_NAME,
        max_tokens=settings.ROUTING_MODEL_MAX_TOKENS,
        timeout=settings.ROUTING_MODEL_TIMEOUT,
        temperature=0,
    )


def create_efficient_model(streaming: bool = True) -> ChatOpenAI:
    return ChatOpenAI(
        api_key=settings.DEFAULT_MODEL_API_KEY,
        base_url=settings.DEFAULT_MODEL_BASE_URL,
        model=settings.DEFAULT_MODEL_NAME,
        max_tokens=settings.DEFAULT_MODEL_MAX_TOKENS,
        timeout=settings.DEFAULT_MODEL_TIMEOUT,
        streaming=streaming,
    )

def create_reasoning_model(streaming: bool = True) -> ChatOpenAI:
    return ChatOpenAI(
        api_key=settings.REASONING_MODEL_API_KEY,
        base_url=settings.REASONING_MODEL_BASE_URL,
        model=settings.REASONING_MODEL_NAME,
        max_tokens=settings.REASONING_MODEL_MAX_TOKENS,
        timeout=settings.REASONING_MODEL_TIMEOUT,
        streaming=streaming,
    )


def create_vector_model() -> OpenAIEmbeddings:
    return OpenAIEmbeddings(
        api_key=settings.VECTOR_MODEL_API_KEY,
        base_url=settings.VECTOR_MODEL_BASE_URL,
        model=settings.VECTOR_MODEL_NAME,
        dimensions=settings.VECTOR_MODEL_DIMENSION,
        request_timeout=settings.VECTOR_MODEL_TIMEOUT,
        check_embedding_ctx_length=False
    )
