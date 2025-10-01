from ..parsers.whatsapp_parser import WhatsAppParser
from ..models.user_model import UserModel
from ..utils.config import ModelConfig


class TrainingService:
    __slots__ = ('_parser', '_config')

    def __init__(self, config: ModelConfig) -> None:
        self._parser = WhatsAppParser()
        self._config = config

    def train_user_model(self, user_id: str, chat_data: bytes) -> tuple[bool, str, int]:
        try:
            messages = list(self._parser.parse_bytes(chat_data))

            if not messages:
                raise ValueError("No valid messages found in chat data")

            texts = [msg.content for msg in messages if len(msg.content) > 5]
            if not texts:
                raise ValueError("No substantial messages for training")
            model = UserModel(
                user_id=user_id,
                model_path=self._config.cache_dir,
                model_name=self._config.model_name
            )

            samples_processed = model.train(texts, epochs=3)
            model.save()

            return True, f"Successfully trained on {len(texts)} messages", samples_processed
        except Exception as e:
            return False, f"Training failed: {str(e)}", 0
