package com.example.auth;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

public class AuthClient {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        
        AuthServiceGrpc.AuthServiceBlockingStub stub = AuthServiceGrpc.newBlockingStub(channel);
        
        // Login
        LoginResponse loginResponse = stub.login(LoginRequest.newBuilder()
                .setUsername("admin")
                .setPassword("admin123")
                .build());
        
        System.out.println("Login successful. Access token: " + loginResponse.getAccessToken());
        
        // Call secure endpoint
        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), 
                "Bearer " + loginResponse.getAccessToken());
        
        AuthServiceGrpc.AuthServiceBlockingStub securedStub = MetadataUtils.attachHeaders(stub, headers);
        
        SecureResponse secureResponse = securedStub.secureEndpoint(SecureRequest.newBuilder()
                .setMessage("Hello secure world!")
                .build());
        
        System.out.println("Secure response: " + secureResponse.getMessage());
        System.out.println("User info: " + secureResponse.getUserInfo());
        
        channel.shutdown();
    }
}