import logging
import sys
import traceback
from datetime import datetime
from logging.handlers import RotatingFileHandler
from pathlib import Path
from typing import Optional, Dict, Any

from pydantic import BaseModel, Field

from app.schemas.log import GenerationLogEntry
from config.settings import Settings


class LogEntry(BaseModel):
    timestamp: str = Field(default_factory=lambda: datetime.utcnow().isoformat())
    level: str
    service: str
    message: str
    context: Optional[Dict[str, Any]] = None
    error: Optional[str] = None
    stack_trace: Optional[str] = None


class LoggerService:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.log_dir = Path(settings.log_dir).absolute()
        self._setup_logging()

    def _setup_logging(self):
        self.log_dir.mkdir(parents=True, exist_ok=True)

        self.logger = logging.getLogger("llm_service")
        self.logger.setLevel(logging.DEBUG if self.settings.debug else logging.INFO)

        formatter = logging.Formatter(
            fmt='%(asctime)s | %(levelname)-8s | %(name)s | %(message)s',
            datefmt='%Y-%m-%d %H:%M:%S'
        )

        generation_log = self.log_dir / "generation.log"
        file_handler = RotatingFileHandler(
            generation_log,
            maxBytes=10 * 1024 * 1024,
            backupCount=5,
            encoding='utf-8'
        )
        file_handler.setFormatter(formatter)

        if self.settings.debug:
            console_handler = logging.StreamHandler()
            console_handler.setFormatter(formatter)
            self.logger.addHandler(console_handler)

        self.logger.addHandler(file_handler)

        logging.basicConfig(handlers=[file_handler], level=logging.ERROR)
        self._setup_exception_logging()

    def _setup_exception_logging(self):

        def handle_exception(exc_type, exc_value, exc_traceback):
            if issubclass(exc_type, KeyboardInterrupt):
                sys.__excepthook__(exc_type, exc_value, exc_traceback)
                return

            self.logger.critical(
                "Uncaught exception",
                exc_info=(exc_type, exc_value, exc_traceback)
            )

        sys.excepthook = handle_exception

    def _prepare_log_entry(
            self,
            level: str,
            message: str,
            context: Optional[Dict] = None,
            error: Optional[Exception] = None
    ) -> str:
        entry = LogEntry(
            level=level,
            service=self.settings.app_name,
            message=message,
            context=context,
            error=str(error) if error else None,
            stack_trace=traceback.format_exc() if error else None
        )
        return entry.model_dump_json()

    def log(
            self,
            level: str,
            message: str,
            context: Optional[Dict] = None,
            error: Optional[Exception] = None
    ):
        log_entry = self._prepare_log_entry(level, message, context, error)
        getattr(self.logger, level)(log_entry)

    def log_generation(self, log_entry: GenerationLogEntry):
        try:
            context = {
                "prompt": log_entry.prompt[:200] + "..." if len(log_entry.prompt) > 200 else log_entry.prompt,
                "status": log_entry.status,
                "action": log_entry.action,
                "generation_time_ms": getattr(log_entry, "generation_time_ms", None)
            }
            self.log(
                level="info" if log_entry.status == "completed" else "error",
                message=f"Text generation {log_entry.status}",
                context=context
            )
        except Exception as e:
            self.logger.error(f"Generation logging failed: {str(e)}")

    def log_error(self, message: str, error: Optional[Exception] = None, context: Optional[Dict] = None):
        self.log("error", message, context, error)

    def log_warning(self, message: str, context: Optional[Dict] = None):
        self.log("warning", message, context)

    def log_info(self, message: str, context: Optional[Dict] = None):
        self.log("info", message, context)

    def log_debug(self, message: str, context: Optional[Dict] = None):
        if self.settings.debug:
            self.log("debug", message, context)

logger_service = LoggerService(settings=Settings())