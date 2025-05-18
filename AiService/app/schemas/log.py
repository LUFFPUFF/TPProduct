from pydantic import BaseModel
from datetime import datetime
from typing import Optional

class GenerationLogEntry(BaseModel):
    prompt: str
    result: Optional[str] = None
    error_message: Optional[str] = None
    timestamp: datetime
    action: str
    status: str
    generation_time_ms: Optional[float] = None