/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package io.github.whatstyle.mcp_server.config;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.whatstyle.grpc.AIServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
/**
 *
 * @author parth
 */


 @Configuration
public class GrpcClientConfig {
    @Value("${grpc.client.ai-service.address:static://localhost:50051}")
    private String aiServiceAddress;

    @Bean
    public ManagedChannel aiServiceChannel() {
        String address = aiServiceAddress.replace("static://", "");
        String[] parts = address.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 50051;

        return ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .keepAliveTime(30, TimeUnit.SECONDS)
            .keepAliveTimeout(5, TimeUnit.SECONDS)
            .build();
    }

    @Bean
    public AIServiceGrpc.AIServiceBlockingStub aiServiceStub(ManagedChannel aiServiceChannel) {
        return AIServiceGrpc.newBlockingStub(aiServiceChannel);
    }
}
