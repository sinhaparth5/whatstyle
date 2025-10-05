/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package io.github.whatstyle.mcp_server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author parth
 */

 @Data
 @Builder
 @NoArgsConstructor
 @AllArgsConstructor
public class ChatResponse {
    private String response;
    private float confidence;
    private long processingTimeMs;
}
