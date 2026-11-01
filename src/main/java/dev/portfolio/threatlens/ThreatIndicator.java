package dev.portfolio.threatlens;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ThreatIndicator(
        UUID id,
        String type,
        String value,
        String source,
        int confidence,
        String status,
        String techniqueId,
        String techniqueName,
        Instant firstSeen,
        Instant expiresAt,
        List<String> tags) {
    public ThreatIndicator {
        tags = List.copyOf(tags);
    }
}