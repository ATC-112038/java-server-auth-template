public class server-interc {
    
}
package com.example.auth;

import io.grpc.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import java.util.Objects;

public class JwtServerInterceptor implements ServerInterceptor {
    public static final Context.Key<String> USERNAME = Context.key("username");
    public static final Context.Key<String> ROLE = Context.key("role");
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        // Skip authentication for login and refresh token endpoints
        if (call.getMethodDescriptor().getFullMethodName().equals("auth.AuthService/Login") ||
            call.getMethodDescriptor().getFullMethodName().equals("auth.AuthService/RefreshToken") ||
            call.getMethodDescriptor().getFullMethodName().equals("auth.AuthService/Logout")) {
            return next.startCall(call, headers);
        }
        
        String token = headers.get(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER));
        
        if (token == null || !token.startsWith("Bearer ")) {
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid Authorization header"), headers);
            return new ServerCall.Listener<>() {};
        }
        
        token = token.substring(7).trim();
        
        if (!JwtUtil.validateToken(token)) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid or expired token"), headers);
            return new ServerCall.Listener<>() {};
        }
        
        String username = JwtUtil.getUsernameFromToken(token);
        String role = JwtUtil.getRoleFromToken(token);
        
        Context context = Context.current()
                .withValue(USERNAME, username)
                .withValue(ROLE, role);
        
        return Contexts.interceptCall(context, call, headers, next);
    }
}