from time import perf_counter
from ..models.user_model import UserModel
from ..utils.config import ModelConfig

class ChatService:
    __slots__ = ('_models', '_config')

    def __init__(self, config: ModelConfig) -> None:
        self._models: dict[str, UserModel] = {}
        self._config = config

    def get_or_create_model(self, user_id: str) -> UserModel:
        if user_id not in self._models:
            self._models[user_id] = UserModel(
                user_id=user_id,
                model_path=self._config.cache_dir,
                model_name=self._config.model_name
            )
        return self._models[user_id]

    def generate_response(self, user_id: str, message: str) -> tuple[str, float, int]:
        start = perf_counter()

        model = self.get_or_create_model(user_id)
        response = model.generate(
            prompt=message,
            max_length=self._config.max_length,
            temperature=self._config.temperature
        )

        processing_time = int((perf_counter() - start) * 1000)
        confidence = 0.85

        return response, confidence, processing_time


