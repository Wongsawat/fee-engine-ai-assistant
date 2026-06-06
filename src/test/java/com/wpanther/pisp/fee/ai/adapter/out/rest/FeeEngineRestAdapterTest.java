package com.wpanther.pisp.fee.ai.adapter.out.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.wpanther.pisp.fee.ai.application.port.out.DryRunResult;
import com.wpanther.pisp.fee.ai.application.port.out.FeeEngineClientException;
import com.wpanther.pisp.fee.ai.application.port.out.FeeRuleResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.*;

class FeeEngineRestAdapterTest {

    private WireMockServer wm;
    private FeeEngineRestAdapter adapter;

    @BeforeEach
    void setUp() {
        wm = new WireMockServer(options().dynamicPort());
        wm.start();
        RestClient client = RestClient.builder().baseUrl(wm.baseUrl()).build();
        adapter = new FeeEngineRestAdapter(client, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        wm.stop();
    }

    @Test
    void dryRun200IsPassedAndForwardsBearer() {
        wm.stubFor(post("/admin/fee-rules/dry-run")
                .withHeader("Authorization", equalTo("Bearer tok123"))
                .willReturn(okJson("{\"charges\":[]}")));
        DryRunResult result = adapter.dryRun("{\"feeType\":\"FREE\"}", "tok123");
        assertThat(result.passed()).isTrue();
        wm.verify(postRequestedFor(urlEqualTo("/admin/fee-rules/dry-run"))
                .withHeader("Authorization", equalTo("Bearer tok123")));
    }

    @Test
    void dryRun400IsFailed() {
        wm.stubFor(post("/admin/fee-rules/dry-run")
                .willReturn(aResponse().withStatus(400).withBody("{\"detail\":\"bad\"}")));
        DryRunResult result = adapter.dryRun("{}", "tok");
        assertThat(result.passed()).isFalse();
    }

    @Test
    void create201ReturnsId() {
        UUID id = UUID.randomUUID();
        wm.stubFor(post("/admin/fee-rules")
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + id + "\"}")));
        FeeRuleResult result = adapter.create("{}", "tok");
        assertThat(result.id()).isEqualTo(id);
    }

    @Test
    void update404ThrowsFeeEngineClientException() {
        wm.stubFor(put(urlMatching("/admin/fee-rules/.*"))
                .willReturn(aResponse().withStatus(404).withBody("{\"detail\":\"gone\"}")));
        assertThatThrownBy(() -> adapter.update(UUID.randomUUID(), "{}", "tok"))
                .isInstanceOf(FeeEngineClientException.class)
                .satisfies(e -> assertThat(((FeeEngineClientException) e).status()).isEqualTo(404));
    }
}
