from pydantic import BaseModel
from datetime import datetime
from typing import Optional

class GenerationMetadata(BaseModel):
    model_name: str
    generation_time_ms: float
    timestamp: datetime
    token_count: Optional[int] = None
    device: Optional[str] = None