package dev.portfolio.threatlens;

import java.time.Instant;
import java.util.UUID;

public record ThreatMatch(UUID id, UUID indicatorId, String observable, String asset, String context, Instant observedAt) {
}