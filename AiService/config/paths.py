from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
MODEL_PATH = str(BASE_DIR / "models")
LOG_DIR = str(BASE_DIR / "logs")