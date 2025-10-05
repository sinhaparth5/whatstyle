/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package io.github.whatstyle.mcp_server.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.whatstyle.mcp_server.dto.ChatRequest;
import io.github.whatstyle.mcp_server.dto.ChatResponse;
import io.github.whatstyle.mcp_server.dto.TrainingResponse;
import io.github.whatstyle.mcp_server.service.ChatService;
import io.github.whatstyle.mcp_server.service.TrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author parth
 */

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final TrainingService trainingService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request from user: {}", request.getUserId());
        ChatResponse response = chatService.processMessage(
                request.getUserId(),
                request.getMessage(),
                request.getContext()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/train", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrainingResponse> train(
            @RequestParam("userId") String userId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        
        log.info("Received training request for user: {}", userId);
        TrainingResponse response = trainingService.trainModel(userId, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("MCP Server is running");
    }
}
