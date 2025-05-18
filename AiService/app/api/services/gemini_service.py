import requests

from app.schemas.generate.request import TextGenerationRequest


def _format_prompt(request: TextGenerationRequest) -> str:
    return request.prompt


class GeminiService:
    def __init__(self, settings_obj):
        self.settings = settings_obj
        self.api_key = self.settings.gemini_api_key
        self.model = "gemini-2.0-flash"
        self.base_url = "https://generativelanguage.googleapis.com/v1beta/models/"
        self.proxy_url = self.settings.proxy_url
        self.use_proxy = self.settings.use_proxy
        self.headers = {
            "Content-Type": "application/json",
        }
        self.timeout = self.settings.request_timeout

    def _get_api_url(self) -> str:
        return f"{self.base_url}{self.model}:generateContent?key={self.api_key}"

    def _send_request(self, payload: dict) -> dict:
        """Отправляет запрос через ScraperAPI"""
        original_url = self._get_api_url()

        try:
            if self.settings.use_proxy:
                proxy_url = (
                    f"{self.settings.proxy_url}?"
                    f"api_key={self.settings.proxy_api_key}&"
                    f"url={original_url}&"
                    f"country_code={self.settings.proxy_country_code}"
                )

                response = requests.post(
                    proxy_url,
                    headers=self.headers,
                    json=payload,
                    timeout=self.timeout
                )
            else:
                logger.info(f"Sending direct request to {original_url}")
                response = requests.post(
                    original_url,
                    headers=self.headers,
                    json=payload,
                    timeout=self.timeout
                )

            response.raise_for_status()
            return response.json()

        except requests.exceptions.RequestException as e:
            error_msg = f"Gemini API request failed: {str(e)}"
            if hasattr(e, 'response') and e.response is not None:
                error_msg += f", Response: {e.response.text}"
            logger.error(error_msg)
            raise RuntimeError(error_msg)

    def generate(self, request: TextGenerationRequest) -> str:
        payload = {
            "contents": [{
                "parts": [{
                    "text": _format_prompt(request)
                }]
            }],
            "generationConfig": {
                "temperature": request.temperature or self.settings.default_temperature,
                "topP": request.top_p or self.settings.default_top_p,
                "maxOutputTokens": request.max_new_tokens or self.settings.default_max_new_tokens,
            }
        }

        try:
            response = self._send_request(payload)
            candidates = response.get("candidates", [{}])
            if not candidates:
                return ""

            parts = candidates[0].get("content", {}).get("parts", [])
            if not parts:
                return ""

            generated_text = parts[0].get("text", "")
            return generated_text.strip().strip('"').strip("'")

        except Exception as e:
            raise RuntimeError(f"Gemini processing error: {str(e)}")

