from pydantic import BaseModel, Field

class APIErrorResponse(BaseModel):
    detail: str = Field(..., example="CUDA out of memory")
    code: int = Field(..., example=500)