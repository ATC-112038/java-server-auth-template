public package com.example.auth.monitoring;

import io.grpc.*;
import io.micrometer.core.instrument.*;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicInteger;

public class ResourceMonitor {
    private static final PrometheusMeterRegistry registry = new PrometheusMeterRegistry();
    private static final AtomicInteger activeRequests = new AtomicInteger(0);
    private static final int MAX_CONCURRENT_REQUESTS = 1000;
    private static final double MAX_CPU_USAGE = 0.9; // 90%
    private static final double MAX_MEMORY_USAGE = 0.9; // 90%

    static {
        // Register JVM metrics
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        Gauge.builder("jvm.memory.used", memoryBean, bean -> bean.getHeapMemoryUsage().getUsed())
                .register(registry);
        Gauge.builder("jvm.memory.max", memoryBean, bean -> bean.getHeapMemoryUsage().getMax())
                .register(registry);
        Gauge.builder("system.cpu.usage", osBean, bean -> bean.getSystemLoadAverage() / osBean.getAvailableProcessors())
                .register(registry);
        Gauge.builder("grpc.active.requests", activeRequests, AtomicInteger::get)
                .register(registry);
    }

    public static PrometheusMeterRegistry getRegistry() {
        return registry;
    }

    public static ServerInterceptor getMonitoringInterceptor() {
        return new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {
                
                // Check system capacity before processing
                if (!hasCapacity()) {
                    call.close(Status.RESOURCE_EXHAUSTED.withDescription("Server capacity exceeded"), headers);
                    return new ServerCall.Listener<>() {};
                }

                activeRequests.incrementAndGet();
                
                return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                        next.startCall(call, headers)) {
                    @Override
                    public void onComplete() {
                        activeRequests.decrementAndGet();
                        super.onComplete();
                    }

                    @Override
                    public void onCancel() {
                        activeRequests.decrementAndGet();
                        super.onCancel();
                    }
                };
            }
        };
    }

    private static boolean hasCapacity() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsage = (double) usedMemory / maxMemory;

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuUsage = osBean.getSystemLoadAverage() / osBean.getAvailableProcessors();

        return activeRequests.get() < MAX_CONCURRENT_REQUESTS &&
               memoryUsage < MAX_MEMORY_USAGE &&
               cpuUsage < MAX_CPU_USAGE;
    }
} {
    
}
