import logging
import os

import torch
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM


class ModelLoaderService:
    def __init__(self, settings_obj):
        self.settings = settings_obj
        self.model_name = self.settings.model_name
        self.model = None
        self.tokenizer = None
        self.logger = logging.getLogger(__name__)
        os.environ["HF_TOKEN"] = self.settings.hf_token

    def load_model(self):
        try:
            os.makedirs(self.settings.model_cache_dir, exist_ok=True)

            self.tokenizer = AutoTokenizer.from_pretrained(
                self.model_name,
                cache_dir=os.path.join(self.settings.model_cache_dir, "tokenizers", self.model_name.replace("/", "_")),
                trust_remote_code=self.settings.trust_remote_code
            )

            self.model = AutoModelForSeq2SeqLM.from_pretrained(
                self.model_name,
                cache_dir=os.path.join(self.settings.model_cache_dir, "models", self.model_name.replace("/", "_")),
                torch_dtype=torch.float32,
                load_in_4bit=False,
                load_in_8bit=False,
                trust_remote_code=self.settings.trust_remote_code
            )

            self.model.to(self.settings.device)

            metadata = {
                "model_name": self.model.config._name_or_path,
                "device": str(self.model.device),
                "cache_dir": self.settings.model_cache_dir
            }

            return self.model, self.tokenizer, metadata

        except Exception as e:
            self.logger.error(f"Ошибка загрузки: {str(e)}")
            raise RuntimeError(f"Не удалось загрузить модель: {str(e)}")


