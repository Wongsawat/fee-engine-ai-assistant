package com.wpanther.pisp.fee.ai.infrastructure.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JsonCanonicaliser {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    @SuppressWarnings("unchecked")
    public String canonicalise(String json) {
        try {
            Map<String, Object> map = mapper.readValue(json, Map.class);
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new CanonicalisationException("Invalid JSON: " + e.getOriginalMessage(), e);
        }
    }
}
