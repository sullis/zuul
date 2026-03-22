/*
 * Copyright 2020 Netflix, Inc.
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
package com.netflix.zuul.message;

import com.netflix.zuul.context.SessionContext;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

public class ZuulMessageBodyBenchmark {

    @State(Scope.Thread)
    public static class GetBodyAsText {
        @Param({"0", "32", "1024", "16384"})
        public int bodySizeBytes;

        private ZuulMessageImpl message;

        @Setup
        public void setUp() {
            message = new ZuulMessageImpl(new SessionContext());
            if (bodySizeBytes == 0) {
                message.setBodyAsText("");
            } else {
                message.setBodyAsText("a".repeat(bodySizeBytes));
            }
        }

        @TearDown
        public void tearDown() {
            message.disposeBufferedBody();
        }

        @Benchmark
        @BenchmarkMode(Mode.AverageTime)
        @OutputTimeUnit(TimeUnit.NANOSECONDS)
        public byte[] getBody() {
            return message.getBody();
        }

        @Benchmark
        @BenchmarkMode(Mode.AverageTime)
        @OutputTimeUnit(TimeUnit.NANOSECONDS)
        public String getBodyAsText() {
            return message.getBodyAsText();
        }
    }
}
