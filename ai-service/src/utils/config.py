from dataclasses import dataclass

@dataclass(frozen=True, slots=True)
class ServerConfig:
    host: str = "0.0.0.0"
    port: int = 50051
    max_workers: int = 10

@dataclass(frozen=True, slots=True)
class ModelConfig:
    model_name: str = "gpt2"
    max_length: int = 512
    temperature: float = 0.7
    cache_dir: str = "./model_cache"
