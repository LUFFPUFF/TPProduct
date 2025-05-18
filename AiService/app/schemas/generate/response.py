from pydantic import BaseModel, Field
from app.schemas.generate.metadata import GenerationMetadata

class TextGenerationResponse(BaseModel):
    result: str = Field(..., description="Сгенерированный текст")
    metadata: GenerationMetadata