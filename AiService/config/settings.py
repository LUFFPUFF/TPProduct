from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings

BASE_DIR = Path(__file__).resolve().parent.parent
DOTENV_FILE = BASE_DIR / ".env"

class Settings(BaseSettings):
    hf_token: str
    gemini_api_key: str
    proxy_api_key: str

    app_name: str = "LLM Inference API"
    version: str = "1.0.0"
    debug: bool = False

    gemini_model: str = "gemini-2.0-flash"
    proxy_url: str = "https://api.scraperapi.com"
    use_proxy: bool = True
    proxy_country_code: str = "us"

    request_timeout: int = 30

    model_name: str = "UrukHan/t5-russian-spell"
    deepseek_model: str = "deepseek-ai/DeepSeek-R1"
    model_path: str = Field(
        default=str(Path(__file__).parent.parent / "models"))
    model_cache_dir: str = Field(
        default=str(Path(__file__).parent.parent / "model_cache"))

    load_in_4bit: bool = False
    load_in_8bit: bool = False
    trust_remote_code: bool = True
    device: str = "cpu"

    default_temperature: float = 0.7
    default_max_new_tokens: int = 150
    default_top_p: float = 0.9
    default_repetition_penalty: float = 1.1

    log_dir: str = Field(default=str(Path(__file__).parent.parent / "logs"))
    data_dir: str = Field(default=str(Path(__file__).parent.parent / "data"))

    server_host: str = "0.0.0.0"
    server_port: int = 8000
    debug_mode: bool = False
    workers_count: int = 1

    class Config:
        env_file = DOTENV_FILE
        env_file_encoding = "utf-8"
        env_prefix = "LLM_"

settings = Settings()