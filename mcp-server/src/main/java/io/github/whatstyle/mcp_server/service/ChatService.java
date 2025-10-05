/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package io.github.whatstyle.mcp_server.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import io.github.whatstyle.mcp_server.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author parth
 */

 @Slf4j
 @Service
 @RequiredArgsConstructor
public class ChatService {
    private final AiGrpcClient aiGrpcClient;

    public ChatResponse processMessage(String userId, String message, Map<String, String> context) {
        log.debug("Processing message for user: {}", userId);

        io.github.whatstyle.grpc.ChatResponse grpcResponse = aiGrpcClient.generateResponse(userId, message, context);

        return ChatResponse.builder()
                .response(grpcResponse.getResponse())
                .confidence(grpcResponse.getConfidence())
                .processingTimeMs(grpcResponse.getProcessingTimeMs())
                .build();
    }
}
