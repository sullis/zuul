package com.netflix.netty.common.metrics;

import com.google.common.truth.Truth;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class CustomLeakDetector extends InstrumentedResourceLeakDetector {
    public static final List<CustomLeakDetector> GLOBAL_REGISTRY = new CopyOnWriteArrayList<>();

    public static void assertZeroLeaks() {
        List<CustomLeakDetector> leaks = GLOBAL_REGISTRY.stream()
                            .filter(detector -> detector.leakCounter.get() > 0)
                            .collect(Collectors.toList());
        Truth.assertThat(leaks).isEmpty();
    }
    public CustomLeakDetector(Class<?> resourceType, int samplingInterval) {
        super(resourceType, samplingInterval);
        GLOBAL_REGISTRY.add(this);
    }

}