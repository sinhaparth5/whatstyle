/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package io.github.whatstyle.mcp_server.service;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.github.whatstyle.mcp_server.dto.TrainingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author parth
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingService {
    private final AiGrpcClient aiGrpcClient;

    public TrainingResponse trainModel(String userId, MultipartFile file) {
        try {
            log.info("Starting training for user: {} with file: {}", userId, file.getOriginalFilename());

            byte[] chatData = file.getBytes();
            String dataFormat = determineDataFormat(file);

            io.github.whatstyle.grpc.TrainingResponse grpcResponse = 
                    aiGrpcClient.trainUserModel(userId, chatData, dataFormat);

            return TrainingResponse.builder()
                    .success(grpcResponse.getSuccess())
                    .message(grpcResponse.getMessage())
                    .samplesProcessed(grpcResponse.getSamplesProcessed())
                    .build();

        } catch (IOException e) {
            log.error("Error reading file for user: {}", userId, e);
            return TrainingResponse.builder()
                    .success(false)
                    .message("Failed to read file: " + e.getMessage())
                    .samplesProcessed(0)
                    .build();
        }
    }

    private String determineDataFormat(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename != null && filename.endsWith(".txt")) {
            return "whatsapp_txt";
        }
        return "unknown";
    }
}
