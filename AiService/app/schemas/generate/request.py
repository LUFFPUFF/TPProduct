from pydantic import BaseModel, Field
from typing import Optional

class TextGenerationRequest(BaseModel):
    prompt: str = Field(..., title="prompt")
    temperature: Optional[float] = Field(0.7, ge=0.0, le=1.0, example=0.7)
    max_new_tokens: Optional[int] = Field(200, ge=1, le=1000, example=200)
    top_p: Optional[float] = Field(1.0, ge=0.0, le=1.0, example=0.95)
    stream: Optional[bool] = Field(False, description="Возвращать ли результат частями (в будущем)")
    do_sample: Optional[bool] = Field(True, description="Включить стохастическую генерацию")
    is_text_generation: Optional[bool] = Field(True, description="True, если требуется генерация текста")