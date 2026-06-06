package com.wpanther.pisp.fee.ai.infrastructure.ai;

import com.wpanther.pisp.fee.ai.infrastructure.config.AiSchemaProperties;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SystemPromptLoaderTest {
    @Test
    void substitutesSchemaVersionFromProperties() {
        var props = new AiSchemaProperties();
        props.setFeeEngineSchemaVersion("V7");
        var loader = new SystemPromptLoader(props);
        assertThat(loader.prompt()).contains("fee-engine-schema-version: V7");
        assertThat(loader.prompt()).doesNotContain("${schemaVersion}");
    }
}
