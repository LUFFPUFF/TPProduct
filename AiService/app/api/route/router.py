print("--- Starting script execution ---")

import uvicorn
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

import logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

logger.info("Starting imports...")
try:
    from app.api.services.gemini_service import GeminiService
    from app.api.services.model_loader import ModelLoaderService
    from app.api.services.text_generator import TextGeneratorService
    from app.schemas.generate.request import TextGenerationRequest
    from app.schemas.generate.response import TextGenerationResponse
    from config.settings import Settings
    logger.info("All modules imported successfully.")
except ImportError as e:
    logger.critical(f"Failed during imports: {e}", exc_info=True)
    print(f"--- CRITICAL: Failed during imports: {e} ---")
    exit(1)

logger.info("Initializing FastAPI app...")
app = FastAPI()
logger.info("FastAPI app initialized.")

logger.info("Adding CORS middleware...")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
logger.info("CORS middleware added.")

try:
    logger.info("Initializing Settings...")
    settings = Settings()
    logger.info("Settings initialized successfully.")
except Exception as e:
    logger.critical(f"Failed to initialize Settings: {e}", exc_info=True)
    print(f"--- CRITICAL: Failed to initialize Settings: {e} ---")
    exit(1)

try:
    logger.info("Initializing ModelLoaderService...")
    model_loader_service = ModelLoaderService(settings)
    logger.info("ModelLoaderService initialized successfully.")
except Exception as e:
    logger.critical(f"Failed to initialize ModelLoaderService: {e}", exc_info=True)
    print(f"--- CRITICAL: Failed to initialize ModelLoaderService: {e} ---")
    exit(1)

try:
    logger.info("Initializing GeminiService...")
    gemini_service = GeminiService(settings)
    logger.info("GeminiService initialized successfully.")
except Exception as e:
    logger.critical(f"Failed to initialize GeminiService: {e}", exc_info=True)
    print(f"--- CRITICAL: Failed to initialize GeminiService: {e} ---")
    exit(1)

try:
    logger.info("Initializing TextGeneratorService...")
    text_generator_service = TextGeneratorService(settings)
    logger.info("TextGeneratorService initialized successfully.")
except Exception as e:
    logger.critical(f"Failed to initialize TextGeneratorService: {e}", exc_info=True)
    print(f"--- CRITICAL: Failed to initialize TextGeneratorService: {e} ---")
    exit(1)

logger.info("All services and settings initialized.")

@app.middleware("http")
async def log_requests(request: Request, call_next):
    logger.info(f"Request: {request.method} {request.url}")
    try:
        response = await call_next(request)
    except Exception as e:
        logger.error(f"Error processing request: {str(e)}", exc_info=True)
        raise
    return response

logger.info("HTTP middleware defined.")

@app.post("/generate", response_model=TextGenerationResponse)
async def generate_text(request: TextGenerationRequest):
    """
    Эндпоинт для генерации текста с использованием Gemini API или локальной модели.
    """
    try:
        logger.info(f"Generating text with prompt: {request.prompt[:100]}...")
        response_object = text_generator_service.generate_text(request)
        logger.info("Text generation successful.")
        return response_object

    except Exception as e:
        logger.error(f"Text generation failed: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Ошибка при генерации текста: {str(e)}")

logger.info("Generate endpoint defined.")

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled exception caught by global handler: {type(exc).__name__}: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"message": f"Internal server error: {exc}"},
    )

logger.info("Global exception handler defined.")
logger.info("Application setup complete.")

if __name__ == "__main__":
    print("--- Entering __main__ block ---")
    try:
        logger.info("Calling uvicorn.run...")
        uvicorn.run(
            app,
            host=settings.server_host,
            port=settings.server_port,
            reload=settings.debug_mode,
            log_level="info"
        )

        logger.info("uvicorn.run finished (this is unexpected for a running server).")
        print("--- uvicorn.run finished ---")
    except Exception as e:
        logger.critical(f"Exception caught when calling uvicorn.run: {str(e)}", exc_info=True)
        print(f"--- CRITICAL: Failed to start server: {str(e)} ---")
    print("--- Exiting script ---")
