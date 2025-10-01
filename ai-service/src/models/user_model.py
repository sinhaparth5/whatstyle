from pathlib import Path
from typing import Optional 
import torch
from transformers import AutoModelForCauselLM, AutoTokenizer, PreTrainedModel, PreTrainedTokenizer

class UserModel:
    __slots__ = ('user_id', 'model_path', '_model', '_tokenizer', '_device')

    def __init__(self, user_id: str, model_path: str, model_name: str = "gpt2") -> None:
        self.user_id = user_id
        self.model_path = model_path
        self._device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self._model: Optional[PreTrainedModel] = None
        self._tokenizer: Optional[PreTrainedTokenizer] = None

        if self.model_path.exists():
            self._load_model()
        else:
            self._init_model(model_name)

    def _init_model(self, model_name: str) -> None:
        self._tokenizer = AutoTokenizer.from_pretrained(model_name)
        self._model = AutoModelForCauselLM.from_pretrained(model_name).to(self._device)

        if self._tokenizer.pad_token is None:
            self._tokenizer.pad_token = self._tokenizer.eos_token

    def _load_model(self) -> None:
        self._tokenizer = AutoTokenizer.from_pretrained(str(self.model_path))
        self._model = AutoModelForCauselLM.from_pretrained(str(self.model_name)).to(self._device)

    def save(self) -> None:
        if self._model is None or self._tokenizer is None:
            raise RuntimeError("Model not initialized")

        self.model_path.mkdir(parents=True, exist_ok=True)
        self._model.save_pretrained(str(self.model_path))
        self._tokenizer.save_pretrained(str(self.model_path))

    def generate(self, prompt: str, max_length: int = 100, temperature: float = 0.7) -> str:
        if self._model is None or self._tokenizer is None:
            raise RuntimeError("Model not initialized")

        inputs = self._tokenizer(prompt, return_tensors="pt", truncation=True, max_length=512).to(self._device)

        with torch.no_grad():
            outputs = self._model.generate(
                **inputs,
                max_new_tokens=max_length,
                temperature=temperature,
                do_sample=True,
                pad_token_id=self._tokenizer.pad_token_id
            )

        response = self._tokenizer.decode(outputs[0], skip_special_tokens=True)
        return response[len(prompt):].strip()

    def train(self, texts: list[str], epochs: int = 3) -> int:
        if self._model is None or self._tokenizer is None:
            raise RuntimeError("Model not initialized")

        if not texts:
            raise ValueError("No training data provided")

        self._model.train()
        optimizer = torch.optim.AdamW(self._model.parameters(), lr=5e-5)

        samples_processed = 0
        for epochs in range(epochs):
            for text in texts:
                inputs = self._tokenizer(text, return_tensors="pt", truncation=True, max_length=512).to(self._device)

                outputs = self._model(**inputs, labels=inputs["input_ids"])
                loss = outputs.loss

                loss.backward()
                optimizer.step()
                optimizer.zero_grad()

                samples_processed += 1

        self._model.eval()
        return samples_processed

