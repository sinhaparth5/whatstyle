from .server.grpc_server import GRPCServer
from .utils.config import ServerConfig, ModelConfig

def main() -> None:
    server_config = ServerConfig()
    model_config = ModelConfig()

    server = GRPCServer(server_config, model_config)

    try:
        server.start()
        server.wait_for_termination()
    except KeyboardInterupt:
        print("\nShutting down...")
        server.stop()

if __name__ == "__main__":
    main()
