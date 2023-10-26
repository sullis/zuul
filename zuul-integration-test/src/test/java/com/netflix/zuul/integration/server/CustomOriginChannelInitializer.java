package com.netflix.zuul.integration.server;

import com.netflix.spectator.api.Registry;
import com.netflix.zuul.netty.connectionpool.ConnectionPoolConfig;
import com.netflix.zuul.netty.connectionpool.DefaultOriginChannelInitializer;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.inject.Inject;

import static com.netflix.zuul.netty.ssl.BaseSslContextFactory.chooseSslProvider;

public class CustomOriginChannelInitializer extends DefaultOriginChannelInitializer {
    @Inject
    public CustomOriginChannelInitializer(ConnectionPoolConfig connPoolConfig, Registry spectatorRegistry) {
        super(connPoolConfig, spectatorRegistry);
    }

    @Override
    protected SslContext getClientSslContext(Registry spectatorRegistry) {
        try {
            SslContext sslContext = SslContextBuilder.forClient()
                    .sslProvider(chooseSslProvider())
                    .trustManager(new TrustAllTrustManager())
                    .build();
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Error configuring SslContext for client request!", e);
        }
    }
}
