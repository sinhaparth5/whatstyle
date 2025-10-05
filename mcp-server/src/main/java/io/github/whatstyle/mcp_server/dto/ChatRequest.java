/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package io.github.whatstyle.mcp_server.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author parth
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Message is required")
    private String message;

    private Map<String, String> context;
}
