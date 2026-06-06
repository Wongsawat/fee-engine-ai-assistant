package com.wpanther.pisp.fee.ai.adapter.out.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpanther.pisp.fee.ai.application.port.out.DryRunResult;
import com.wpanther.pisp.fee.ai.application.port.out.FeeEngineClientException;
import com.wpanther.pisp.fee.ai.application.port.out.FeeEnginePort;
import com.wpanther.pisp.fee.ai.application.port.out.FeeRuleResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class FeeEngineRestAdapter implements FeeEnginePort {

    private final RestClient client;
    private final ObjectMapper mapper;

    public FeeEngineRestAdapter(RestClient feeEngineRestClient, ObjectMapper mapper) {
        this.client = feeEngineRestClient;
        this.mapper = mapper;
    }

    @Override
    public DryRunResult dryRun(String ruleJson, String bearerToken) {
        var response = client.post()
                .uri("/admin/fee-rules/dry-run")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ruleJson)
                .retrieve()
                .onStatus(s -> true, (req, res) -> {})
                .toEntity(String.class);
        int code = response.getStatusCode().value();
        JsonNode body = parse(response.getBody());
        if (code == 200) return new DryRunResult(true, body);
        if (code == 400) return new DryRunResult(false, body);
        throw new FeeEngineClientException(code, body);
    }

    @Override
    public FeeRuleResult create(String ruleJson, String bearerToken) {
        return mutate("/admin/fee-rules", "POST", ruleJson, bearerToken);
    }

    @Override
    public FeeRuleResult update(UUID ruleId, String ruleJson, String bearerToken) {
        return mutate("/admin/fee-rules/" + ruleId, "PUT", ruleJson, bearerToken);
    }

    @Override
    public FeeRuleResult fetchRule(UUID ruleId, String bearerToken) {
        var response = client.get()
                .uri("/admin/fee-rules/" + ruleId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .retrieve()
                .onStatus(s -> true, (req, res) -> {})
                .toEntity(String.class);
        return toRuleResult(response.getStatusCode().value(), response.getBody());
    }

    private FeeRuleResult mutate(String uri, String method, String ruleJson, String bearerToken) {
        RestClient.RequestBodySpec spec = ("PUT".equals(method)
                ? client.put() : client.post())
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON);
        var response = spec.body(ruleJson)
                .retrieve()
                .onStatus(s -> true, (req, res) -> {})
                .toEntity(String.class);
        return toRuleResult(response.getStatusCode().value(), response.getBody());
    }

    private FeeRuleResult toRuleResult(int code, String body) {
        JsonNode node = parse(body);
        if (code >= 200 && code < 300) {
            UUID id = node != null && node.hasNonNull("id")
                    ? UUID.fromString(node.get("id").asText()) : null;
            return new FeeRuleResult(id, node);
        }
        throw new FeeEngineClientException(code, node);
    }

    private JsonNode parse(String body) {
        if (body == null || body.isBlank()) return null;
        try { return mapper.readTree(body); }
        catch (Exception e) { return null; }
    }
}
