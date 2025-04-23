public package com.example.auth;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthServer {
    private static final Map<String, User> USERS = new ConcurrentHashMap<>();
    private static final Map<String, String> REFRESH_TOKENS = new ConcurrentHashMap<>();
    
    static {
        // Initialize with some test users
        USERS.put("admin", new User("admin", "admin123", "ADMIN"));
        USERS.put("user", new User("user", "user123", "USER"));
    }
    
    private Server server;
    
    public void start(int port) throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new AuthServiceImpl())
                .intercept(new JwtServerInterceptor())
                .build()
                .start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Shutting down gRPC server");
            AuthServer.this.stop();
        }));
    }
    
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
    
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
    
    static class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {
        @Override
        public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
            User user = USERS.get(request.getUsername());
            
            if (user == null || !user.getPassword().equals(request.getPassword())) {
                responseObserver.onError(Status.UNAUTHENTICATED
                        .withDescription("Invalid username or password")
                        .asRuntimeException());
                return;
            }
            
            String accessToken = JwtUtil.generateAccessToken(user.getUsername(), user.getRole());
            String refreshToken = JwtUtil.generateRefreshToken(user.getUsername());
            
            // Store refresh token
            REFRESH_TOKENS.put(refreshToken, user.getUsername());
            
            LoginResponse response = LoginResponse.newBuilder()
                    .setAccessToken(accessToken)
                    .setRefreshToken(refreshToken)
                    .setExpiresIn(JwtUtil.ACCESS_TOKEN_EXPIRATION_MINUTES * 60)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        
        @Override
        public void refreshToken(RefreshRequest request, StreamObserver<RefreshResponse> responseObserver) {
            String refreshToken = request.getRefreshToken();
            
            if (!REFRESH_TOKENS.containsKey(refreshToken) || !JwtUtil.validateToken(refreshToken)) {
                responseObserver.onError(Status.UNAUTHENTICATED
                        .withDescription("Invalid refresh token")
                        .asRuntimeException());
                return;
            }
            
            String username = REFRESH_TOKENS.get(refreshToken);
            User user = USERS.get(username);
            
            String newAccessToken = JwtUtil.generateAccessToken(user.getUsername(), user.getRole());
            
            RefreshResponse response = RefreshResponse.newBuilder()
                    .setAccessToken(newAccessToken)
                    .setExpiresIn(JwtUtil.ACCESS_TOKEN_EXPIRATION_MINUTES * 60)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        
        @Override
        public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
            String refreshToken = request.getRefreshToken();
            
            if (REFRESH_TOKENS.containsKey(refreshToken)) {
                REFRESH_TOKENS.remove(refreshToken);
            }
            
            LogoutResponse response = LogoutResponse.newBuilder()
                    .setSuccess(true)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        
        @Override
        public void secureEndpoint(SecureRequest request, StreamObserver<SecureResponse> responseObserver) {
            // This method is protected by JwtServerInterceptor
            String username = JwtServerInterceptor.USERNAME.get();
            String role = JwtServerInterceptor.ROLE.get();
            
            SecureResponse response = SecureResponse.newBuilder()
                    .setMessage("Secure response: " + request.getMessage())
                    .setUserInfo("User: " + username + ", Role: " + role)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        AuthServer server = new AuthServer();
        server.start(50051);
        System.out.println("Server started on port 50051");
        server.blockUntilShutdown();
    }
} {
    
}
