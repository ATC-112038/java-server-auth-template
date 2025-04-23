package com.example.auth;

import com.example.auth.monitoring.ResourceMonitor;
import grpc.health.v1.HealthGrpc;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;

public class AuthServer {
    // ... existing fields ...

    private HealthStatusManager healthStatusManager;
    private HTTPServer metricsServer;

    public void start(int port) throws IOException {
        // Initialize monitoring
        new ClassLoaderMetrics().bindTo(ResourceMonitor.getRegistry());
        new JvmMemoryMetrics().bindTo(ResourceMonitor.getRegistry());
        new JvmGcMetrics().bindTo(ResourceMonitor.getRegistry());
        new ProcessorMetrics().bindTo(ResourceMonitor.getRegistry());
        new JvmThreadMetrics().bindTo(ResourceMonitor.getRegistry());

        // Start Prometheus metrics endpoint
        metricsServer = new HTTPServer(8080);

        // Health check service
        healthStatusManager = new HealthStatusManager();
        
        server = ServerBuilder.forPort(port)
                .addService(new AuthServiceImpl())
                .addService(ProtoReflectionService.newInstance()) // For gRPC CLI tools
                .addService(healthStatusManager.getHealthService()) // Health checks
                .intercept(new JwtServerInterceptor())
                .intercept(ResourceMonitor.getMonitoringInterceptor())
                .intercept(new RateLimitingInterceptor())
                .build()
                .start();
        
        // Set health status to SERVING
        healthStatusManager.setStatus(
            HealthGrpc.getServiceDescriptor().getName(), 
            grpc.health.v1.HealthCheckResponse.ServingStatus.SERVING);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Shutting down gRPC server");
            healthStatusManager.setStatus(
                HealthGrpc.getServiceDescriptor().getName(), 
                grpc.health.v1.HealthCheckResponse.ServingStatus.NOT_SERVING);
            AuthServer.this.stop();
        }));
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
        if (metricsServer != null) {
            metricsServer.stop();
        }
    }

    // ... rest of the class ...
}