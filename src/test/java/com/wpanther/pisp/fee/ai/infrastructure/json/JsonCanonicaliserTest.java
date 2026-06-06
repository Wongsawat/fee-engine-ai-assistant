package com.wpanther.pisp.fee.ai.infrastructure.json;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonCanonicaliserTest {

    private final JsonCanonicaliser canonicaliser = new JsonCanonicaliser();

    @Test
    void producesSameOutputRegardlessOfKeyOrderAndWhitespace() {
        String a = canonicaliser.canonicalise("{ \"b\": 1, \"a\": 2 }");
        String b = canonicaliser.canonicalise("{\"a\":2,\"b\":1}");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void throwsCanonicalisationExceptionOnInvalidJson() {
        assertThatThrownBy(() -> canonicaliser.canonicalise("not json"))
                .isInstanceOf(CanonicalisationException.class);
    }
}
