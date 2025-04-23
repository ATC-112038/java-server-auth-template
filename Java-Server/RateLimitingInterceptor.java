package com.example.auth;

import io.grpc.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitingInterceptor implements ServerInterceptor {
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastResetTimes = new ConcurrentHashMap<>();

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        String clientIp = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR).toString();
        
        // Reset counter if minute has passed
        long currentTime = System.currentTimeMillis();
        lastResetTimes.compute(clientIp, (key, lastResetTime) -> {
            if (lastResetTime == null || currentTime - lastResetTime > 60000) {
                requestCounts.put(clientIp, new AtomicInteger(0));
                return currentTime;
            }
            return lastResetTime;
        });
        
        AtomicInteger count = requestCounts.get(clientIp);
        if (count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
            call.close(Status.RESOURCE_EXHAUSTED.withDescription("Rate limit exceeded"), headers);
            return new ServerCall.Listener<>() {};
        }
        
        return next.startCall(call, headers);
    }
}