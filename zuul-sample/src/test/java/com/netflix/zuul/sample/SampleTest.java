package com.netflix.zuul.sample;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;

import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;

public class SampleTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    @Before
    public void before() {
        wireMockRule.resetAll();
        wireMockRule.stubFor(any(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-Hello", "World")
                        .withBody("{}")));
    }

    @Test
    public void happyPath() {
        given()
            .baseUri(wireMockRule.baseUrl())
        .when()
            .get("/whatever")
        .then()
            .statusCode(200)
            .contentType("application/json")
            .header("X-Hello", "World");        
    }
}
