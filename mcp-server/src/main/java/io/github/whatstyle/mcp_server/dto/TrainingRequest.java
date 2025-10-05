/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package io.github.whatstyle.mcp_server.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class TrainingRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Chat data file is required")
    private MultipartFile file;
}
