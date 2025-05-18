FROM python:3.10-slim

WORKDIR /app

ENV PIP_NO_CACHE_DIR=1

COPY requirements.txt .

RUN pip install -r requirements.txt

RUN pip install torch==2.3.0 torchvision==0.18.0 torchaudio==2.3.0 --index-url https://download.pytorch.org/whl/cpu

COPY . .

EXPOSE 8000

CMD ["uvicorn", "app.api.route.router:app", "--host", "0.0.0.0", "--port", "8000"]