from fastapi import HTTPException
from transformers import pipeline

from app.api.services.gemini_service import GeminiService
from app.schemas.generate.request import TextGenerationRequest
from app.schemas.generate.response import TextGenerationResponse
from app.schemas.generate.metadata import GenerationMetadata
import time
from app.api.services.model_loader import ModelLoaderService
from app.api.services.logger import logger_service
from datetime import datetime

from app.schemas.log import GenerationLogEntry

class TextGeneratorService:
    def __init__(self, settings_obj):
        self.settings = settings_obj
        model_loader_service = ModelLoaderService(settings_obj)
        self.model, self.tokenizer, self.metadata = model_loader_service.load_model()

        self.pipeline = pipeline(
            "text2text-generation",
            model=self.model,
            tokenizer=self.tokenizer,
            device=self.settings.device
        )

        self.logger = logger_service
        self.gemini = GeminiService(settings_obj)

    def generate_text(self, request: TextGenerationRequest) -> TextGenerationResponse:
        start_time = time.time()

        self.logger.log_generation(GenerationLogEntry(
            prompt=request.prompt,
            timestamp=datetime.utcnow(),
            action="generate_text",
            status="in_progress"
        ))

        try:
            if request.is_text_generation:
                result = self.gemini.generate(request)
                model_name = "gemini-2.0-flash"
                device = "external_api"
                token_count = len(result.split())
            else:
                outputs = self.pipeline(
                    request.prompt,
                    max_new_tokens=request.max_new_tokens,
                    temperature=request.temperature,
                    top_p=request.top_p,
                    repetition_penalty=self.settings.default_repetition_penalty,
                    do_sample=request.do_sample,
                    pad_token_id=self.tokenizer.pad_token_id if self.tokenizer.pad_token_id is not None else self.tokenizer.eos_token_id
                )
                result = outputs[0]['generated_text'].strip()
                model_name = self.metadata["model_name"]
                device = self.metadata.get("device")
                token_count = len(self.tokenizer.encode(result))

            generation_time_ms = (time.time() - start_time) * 1000
            metadata = GenerationMetadata(
                model_name=model_name,
                generation_time_ms=generation_time_ms,
                timestamp=datetime.utcnow(),
                token_count=token_count,
                device=device
            )

            self.logger.log_generation(GenerationLogEntry(
                prompt=request.prompt,
                result=result,
                timestamp=datetime.utcnow(),
                action="generate_text",
                status="completed",
                generation_time_ms=generation_time_ms
            ))

            return TextGenerationResponse(result=result, metadata=metadata)
        except Exception as e:
            self.logger.log_generation(GenerationLogEntry(
                prompt=request.prompt,
                timestamp=datetime.utcnow(),
                action="generate_text",
                status="failed",
                error_message=str(e)
            ))
            self.logger.error(f"Ошибка при генерации текста для запроса '{request.prompt[:50]}...': {str(e)}",
                              exc_info=True)
            raise HTTPException(status_code=500, detail=f"Ошибка при генерации текста: {str(e)}")