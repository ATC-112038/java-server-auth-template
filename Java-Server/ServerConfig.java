public package com.example.auth.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServerConfig {
    private int port = 50051;
    private int maxConcurrentCalls = 1000;
    private int flowControlWindow = 1048576; // 1MB
    
    // JWT
    private String jwtSecret;
    private int accessTokenExpirationMinutes = 15;
    private int refreshTokenExpirationDays = 7;
    
    // Rate limiting
    private int rateLimitRequestsPerMinute = 1000;
    private int rateLimitBurstCapacity = 100;
    
    // Capacity
    private double maxCpuUsage = 0.9;
    private double maxMemoryUsage = 0.9;
    private int maxConcurrentRequests = 1000;
    
    public static ServerConfig loadFromEnvironment() {
        ServerConfig config = new ServerConfig();
        
        // Load from environment variables with fallback to system properties
        config.setPort(Integer.parseInt(
            System.getenv().getOrDefault("SERVER_PORT", 
            System.getProperty("server.port", "50051"))));
            
        // Load other properties similarly...
        
        return config;
    }
} {
    
}
