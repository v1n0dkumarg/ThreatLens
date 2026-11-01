package dev.portfolio.threatlens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class IndicatorServiceTest {
    @Test
        void normalizesDomainsAndUrls() {
                assertEquals("new.example", IndicatorService.normalizeValue("DOMAIN", "  NEW.Example  "));
                assertEquals("https://example.com/path", IndicatorService.normalizeValue("URL", "HTTPS://Example.COM:443/path#section"));
    }

    @Test
    void rejectsMalformedIndicatorValue() {
        assertThrows(IllegalArgumentException.class,
            () -> IndicatorService.normalizeValue("IP", "999.999.999.999"));
    }
}