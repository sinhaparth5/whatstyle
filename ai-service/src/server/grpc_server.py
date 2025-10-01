from concurrent.futures import ThreadPoolExecutor
import grpc
from ..services.chat_service import ChatService
from ..services.training_service import TrainingService
from ..utils.config import ServerConfig, ModelConfig
from ..generated import ai_service_pb2
from ..generated import ai_service_pb2_grpc

class AIServiceServicer(ai_service_pb2_grpc.AIServiceServicer):
    __slots__ = ('_chat_service', '_training_service')

    def __init__(self, chat_service: ChatService, training_service: TrainingService) -> None:
        self._chat_service = chat_service
        self._training_service = training_service

    def GenerateResponse(self, request: ai_service_pb2.ChatRequest, context: grpc.ServicerContext) -> ai_service_pb2.ChatResponse:
        try:
            response, confidence, processing_time = self._chat_service.generate_response(
                user_id=request.user_id,
                message=request.message
            )
            
            return ai_service_pb2.ChatResponse(
                response=response,
                confidence=confidence,
                processing_time_ms=processing_time
            )
        except Exception as e:
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(str(e))
            raise

    def TrainUserModel(self, request: ai_service_pb2.TrainingRequest, context: grpc.ServicerContext) -> ai_service_pb2.TrainingResponse:
        try:
            success, message, samples = self._training_service.train_user_model(
                user_id=request.user_id,
                chat_data=request.chat_data
            )
            
            return ai_service_pb2.TrainingResponse(
                success=success,
                message=message,
                samples_processed=samples
            )
        except Exception as e:
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(str(e))
            raise

class GRPCServer:
    __slots__ = ('_server', '_config')
    
    def __init__(self, config: ServerConfig, model_config: ModelConfig) -> None:
        self._config = config
        
        chat_service = ChatService(model_config)
        training_service = TrainingService(model_config)
        servicer = AIServiceServicer(chat_service, training_service)
        
        self._server = grpc.server(ThreadPoolExecutor(max_workers=config.max_workers))
        ai_service_pb2_grpc.add_AIServiceServicer_to_server(servicer, self._server)
        self._server.add_insecure_port(f"{config.host}:{config.port}")
    
    def start(self) -> None:
        self._server.start()
        print(f"gRPC server started on {self._config.host}:{self._config.port}")
    
    def wait_for_termination(self) -> None:
        self._server.wait_for_termination()
    
    def stop(self, grace: int = 5) -> None:
        self._server.stop(grace)

