/*
 * Copyright 2023 Netflix, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package com.netflix.zuul.netty;

import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import static io.netty.util.ResourceLeakDetector.LeakListener;

public class NettyLeakListener implements LeakListener {
    private static final String LEAK_COUNTER_NAME = "zuul.netty.leakcount";
    private final Registry registry;

    public NettyLeakListener(final Registry registry) {
        this.registry = registry;
    }

    @Override
    public void onLeak(final String resourceType, final String records) {
        Id counterId = registry.createId(LEAK_COUNTER_NAME)
                          .withTag("resourceType", resourceType);
        this.registry.counter(counterId).increment();
    }

    public long getLeakCount() {
        return this.registry.counter(LEAK_COUNTER_NAME).count();
    }
}