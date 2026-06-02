from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from app.models.schemas import RouteRequest, RouteResponse
from app.router.routing_service import route_agent_type


router = APIRouter()

@router.post("/chat/route", response_model=RouteResponse)
async def route_endpoint(req: RouteRequest) -> RouteResponse:
    return await route_agent_type(req)

# @router.post("/chat/stream")
# async def generate_endpoint(req: GenerateRequest) -> StreamingResponse:
#     return StreamingResponse(stream_generate(req), media_type="text/event-stream")

