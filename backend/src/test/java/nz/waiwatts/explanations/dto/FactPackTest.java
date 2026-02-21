package nz.waiwatts.explanations.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FactPackTest {

    @Test
    void constructorLeavesGeneratedAtUtcUnsetForDeterminism() {
        FactPack factPack = new FactPack();

        assertNull(factPack.getGeneratedAtUtc());
        assertNotNull(factPack.getFacts());
        assertNotNull(factPack.getGuardrails());
    }
}
