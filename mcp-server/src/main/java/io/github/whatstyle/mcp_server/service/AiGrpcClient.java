/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package io.github.whatstyle.mcp_server.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.protobuf.ByteString;

import io.github.whatstyle.grpc.AIServiceGrpc;
import io.github.whatstyle.grpc.ChatRequest;
import io.github.whatstyle.grpc.ChatResponse;
import io.github.whatstyle.grpc.TrainingRequest;
import io.github.whatstyle.grpc.TrainingResponse;
import io.github.whatstyle.mcp_server.exception.AiServiceException;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author parth
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGrpcClient {
    private final AIServiceGrpc.AIServiceBlockingStub aiServiceStub;

    public ChatResponse generateResponse(String userId, String message, Map<String, String> context) {
        try {
            ChatRequest.Builder requestBuilder = ChatRequest.newBuilder()
                    .setUserId(userId)
                    .setMessage(message);

            if (context != null) {
                requestBuilder.putAllContext(context);
            }

            ChatResponse response = aiServiceStub.generateResponse(requestBuilder.build());
            log.info("Generated response for user {} in {}ms", userId, response.getProcessingTimeMs());
            return response;

        } catch (StatusRuntimeException e) {
            log.error("gRPC error while generating response: {}", e.getStatus());
            throw new AiServiceException("Failed to generate response: " + e.getStatus().getDescription());
        }
    }

    public TrainingResponse trainUserModel(String userId, byte[] chatData, String dataFormat) {
        try {
            TrainingRequest request = TrainingRequest.newBuilder()
                    .setUserId(userId)
                    .setChatData(ByteString.copyFrom(chatData))
                    .setDataFormat(dataFormat)
                    .build();

            TrainingResponse response = aiServiceStub.trainUserModel(request);
            log.info("Training completed for user {}: {} samples processed", userId, response.getSamplesProcessed());
            return response;

        } catch (StatusRuntimeException e) {
            log.error("gRPC error while training model: {}", e.getStatus());
            throw new AiServiceException("Failed to train model: " + e.getStatus().getDescription());
        }
    }
}
