package com.netflix.zuul.integration.wiremock;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.InputStreamSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;

import java.io.IOException;
import java.io.InputStream;

public class CustomResponseTransformer extends ResponseTransformer {
    @Override
    public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
        if (!request.getUrl().startsWith("/responseSize")) {
            return response;
        }

        long responseSize = 995;
        QueryParameter responseSizeParam = request.queryParameter("n");
        if (responseSizeParam.isPresent()) {
            responseSize = Long.parseLong(responseSizeParam.firstValue());
        }

        System.out.println("responseSize: " + responseSize);

        final InputStream inputStream = new FixedSizeInputStream(responseSize, 'a');
        final InputStreamSource source = new InputStreamSource() {
            @Override
            public InputStream getStream() {
                return inputStream;
            }
        };

        HttpHeaders headers = new HttpHeaders(new HttpHeader("Content-Type", "text/plain"));

        return Response.response().body(source).headers(headers).build();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    static class FixedSizeInputStream extends InputStream {

        private long remainingBytes;
        private final char data;

        public FixedSizeInputStream(long size, char data) {
            this.remainingBytes = size;
            this.data = data;
        }

        @Override
        public int read() throws IOException {
            System.out.println("read called. remainingBytes=" + remainingBytes);
            if (remainingBytes <= 0) {
                System.out.println("read: returning -1. remainingBytes=" + remainingBytes);
                return -1;
            } else {
                try {
                    return data;
                } finally {
                    remainingBytes--;
                }
            }
        }

        @Override
        public int available() throws IOException {
            int n = 0;
            if (this.remainingBytes == 0) {
                n = 0;
            } else {
                n = (remainingBytes >= 4096) ? 4096 : (int) this.remainingBytes;
            }
            System.out.println("available: n = " + n);
            return n;
        }
    }
}
