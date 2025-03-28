from flask import Flask, request, jsonify
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import torch

app = Flask(__name__)

MODEL_NAME = "ai-forever/ruT5-large"
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
model = AutoModelForSeq2SeqLM.from_pretrained(MODEL_NAME)

def generate_response(prompt, max_length=50, num_return_sequences=1, temperature=1.0, top_k=50, top_p=0.95):
    try:
        inputs = tokenizer(prompt, return_tensors="pt", truncation=True, padding=True)
        with torch.no_grad():
            outputs = model.generate(
                **inputs,
                max_length=max_length,
                num_return_sequences=num_return_sequences,
                temperature=temperature,
                top_k=top_k,
                top_p=top_p
            )
        responses = [tokenizer.decode(output, skip_special_tokens=True) for output in outputs]
        return responses
    except Exception as e:
        return str(e)

@app.route('/generate', methods=['POST'])
def generate_text():
    try:
        data = request.json
        prompt = data.get('prompt', '').strip()
        if not prompt:
            return jsonify({"error": "Prompt is required"}), 400

        max_length = int(data.get('max_length', 50))
        num_return_sequences = int(data.get('num_return_sequences', 1))
        temperature = float(data.get('temperature', 1.0))
        top_k = int(data.get('top_k', 50))
        top_p = float(data.get('top_p', 0.95))

        responses = generate_response(prompt, max_length, num_return_sequences, temperature, top_k, top_p)
        return jsonify({"responses": responses})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)