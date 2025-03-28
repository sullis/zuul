/*
 * Copyright 2018 Netflix, Inc.
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

package com.netflix.netty.common.accesslog;

import com.netflix.netty.common.HttpLifecycleChannelHandler;
import com.netflix.netty.common.SourceAddressChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: michaels@netflix.com
 * Date: 4/14/16
 * Time: 3:51 PM
 */
public final class AccessLogChannelHandler {
    private static final AttributeKey<RequestState> ATTR_REQ_STATE =
            AttributeKey.newInstance("_accesslog_requeststate");

    private static final Logger LOG = LoggerFactory.getLogger(AccessLogChannelHandler.class);

    public static class AccessLogInboundChannelHandler extends ChannelInboundHandlerAdapter {
        private final AccessLogPublisher publisher;

        public AccessLogInboundChannelHandler(AccessLogPublisher publisher) {
            this.publisher = publisher;
        }

        protected Integer getLocalPort(ChannelHandlerContext ctx) {
            return ctx.channel()
                    .attr(SourceAddressChannelHandler.ATTR_SERVER_LOCAL_PORT)
                    .get();
        }

        protected String getRemoteIp(ChannelHandlerContext ctx) {
            return ctx.channel()
                    .attr(SourceAddressChannelHandler.ATTR_SOURCE_ADDRESS)
                    .get();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof HttpRequest) {
                RequestState state = new RequestState();
                state.request = (HttpRequest) msg;
                state.startTimeNs = System.nanoTime();
                state.requestBodySize = 0;
                ctx.channel().attr(ATTR_REQ_STATE).set(state);
            }

            if (msg instanceof HttpContent) {
                RequestState state = ctx.channel().attr(ATTR_REQ_STATE).get();
                if (state != null) {
                    state.requestBodySize += ((HttpContent) msg).content().readableBytes();
                }
            }

            super.channelRead(ctx, msg);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof HttpLifecycleChannelHandler.CompleteEvent) {
                // Get the stored request, and remove the attr from channel to cleanup.
                RequestState state = ctx.channel().attr(ATTR_REQ_STATE).get();
                ctx.channel().attr(ATTR_REQ_STATE).set(null);

                // Response complete, so now write to access log.
                long durationNs = System.nanoTime() - state.startTimeNs;

                Integer localPort = getLocalPort(ctx);
                String remoteIp = getRemoteIp(ctx);

                if (state.response == null) {
                    LOG.debug(
                            "Response null in AccessLog, Complete reason={}, duration={}, url={}, method={}",
                            ((HttpLifecycleChannelHandler.CompleteEvent) evt).getReason(),
                            durationNs / (1000 * 1000),
                            state.request != null ? state.request.uri() : "-",
                            state.request != null ? state.request.method() : "-");
                }

                publisher.log(
                        ctx.channel(),
                        state.request,
                        state.response,
                        state.dateTime,
                        localPort,
                        remoteIp,
                        durationNs,
                        state.requestBodySize,
                        state.responseBodySize);
            }

            super.userEventTriggered(ctx, evt);
        }
    }

    public static final class AccessLogOutboundChannelHandler extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            RequestState state = ctx.channel().attr(ATTR_REQ_STATE).get();

            if (msg instanceof HttpResponse) {
                state.response = (HttpResponse) msg;
                state.responseBodySize = 0;
            }

            if (msg instanceof HttpContent) {
                state.responseBodySize += ((HttpContent) msg).content().readableBytes();
            }

            super.write(ctx, msg, promise);
        }
    }

    private static class RequestState {
        final LocalDateTime dateTime = LocalDateTime.now(ZoneId.systemDefault());
        HttpRequest request;
        HttpResponse response;
        long startTimeNs;
        long requestBodySize = 0;
        long responseBodySize = 0;
    }
}
