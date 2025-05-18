from pydantic import BaseModel, Field
from typing import Dict, Literal, Optional, List, Union
from datetime import datetime

class ComponentStatus(BaseModel):
    """Статус отдельного компонента системы"""
    status: Literal["healthy", "unhealthy", "degraded"]
    model: Optional[str] = None
    device: Optional[str] = None
    test_output: Optional[str] = None
    error: Optional[str] = None
    response_time_ms: Optional[float] = None
    details: Optional[Dict] = None

class HealthCheckResponse(BaseModel):
    """Полный ответ о состоянии системы"""
    status: Literal["healthy", "unhealthy", "degraded"]
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    duration_ms: float
    components: Dict[str, ComponentStatus]
    environment: Dict[str, Optional[Union[str, bool]]]
    suggestions: Optional[List[str]] = None

    class Config:
        json_schema_extra = {
            "example": {
                "status": "healthy",
                "timestamp": "2023-10-15T12:00:00Z",
                "duration_ms": 245.7,
                "components": {
                    "local_model": {
                        "status": "healthy",
                        "model": "UrukHan/t5-russian-spell",
                        "device": "cuda:0",
                        "test_output": "Модель работает нормально",
                        "details": {
                            "dtype": "torch.float16",
                            "quantization": "4bit"
                        }
                    },
                    "deepseek_api": {
                        "status": "healthy",
                        "model": "deepseek-ai/DeepSeek-R1",
                        "response_time_ms": 128.4
                    }
                },
                "environment": {
                    "device": "cuda",
                    "debug_mode": False,
                    "model_cache": "/app/model_cache"
                },
                "suggestions": [
                    "Обновить CUDA драйверы для лучшей производительности"
                ]
            }
        }