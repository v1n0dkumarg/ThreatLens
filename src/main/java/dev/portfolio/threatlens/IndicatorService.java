package dev.portfolio.threatlens;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import jakarta.annotation.PostConstruct;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IndicatorService {
    private static final Pattern IPV4 = Pattern.compile("^(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$");
    private static final Pattern DOMAIN = Pattern.compile("(?i)^(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}$");
    private static final Pattern SHA256 = Pattern.compile("(?i)^[a-f0-9]{64}$");
    private static final Pattern EMAIL = Pattern.compile("(?i)^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9-]+(?:\\.[a-z0-9-]+)+$");
    private final JdbcTemplate jdbc;

    public IndicatorService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void seedDemoData() {
        Integer indicatorCount = jdbc.queryForObject("SELECT COUNT(*) FROM threat_indicators", Integer.class);
        if (indicatorCount == null || indicatorCount == 0) {
            ThreatIndicator ip = add(new CreateIndicatorRequest("IP", "203.0.113.77", "Demo feed", 85,
                    "T1071", "Application Layer Protocol", List.of("demo", "network")));
            ThreatIndicator domain = add(new CreateIndicatorRequest("DOMAIN", "login-check.example", "Demo feed", 72,
                    "T1566", "Phishing", List.of("demo", "domain")));
            addMatch(ip, "workstation-17", "Synthetic outbound connection matched a documentation-only IP",
                    Instant.now().minus(18, ChronoUnit.MINUTES));
            addMatch(domain, "identity-gateway", "Synthetic DNS telemetry matched a reserved example domain",
                    Instant.now().minus(43, ChronoUnit.MINUTES));
        }
    }

    @Cacheable("threat-indicators")
    public List<ThreatIndicator> indicators() {
        return jdbc.query("SELECT id, type, value, source, confidence, status, technique_id, technique_name, first_seen, expires_at "
                        + "FROM threat_indicators ORDER BY first_seen DESC",
                (rs, row) -> {
                    UUID id = UUID.fromString(rs.getString("id"));
                    List<String> tags = jdbc.query("SELECT tag FROM indicator_tags WHERE indicator_id = ? ORDER BY position",
                            (tagRs, tagRow) -> tagRs.getString("tag"), id);
                    return new ThreatIndicator(id, rs.getString("type"), rs.getString("value"), rs.getString("source"),
                            rs.getInt("confidence"), rs.getString("status"), rs.getString("technique_id"),
                            rs.getString("technique_name"), rs.getTimestamp("first_seen").toInstant(),
                            rs.getTimestamp("expires_at").toInstant(), tags);
                });
    }

    @Cacheable("threat-matches")
    public List<ThreatMatch> matches() {
        return jdbc.query("SELECT id, indicator_id, observable, asset, context, observed_at "
                        + "FROM threat_matches ORDER BY observed_at DESC",
                (rs, row) -> new ThreatMatch(UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("indicator_id")), rs.getString("observable"),
                        rs.getString("asset"), rs.getString("context"), rs.getTimestamp("observed_at").toInstant()));
    }

    @Transactional
    @CacheEvict(cacheNames = "threat-indicators", allEntries = true)
    public synchronized ThreatIndicator create(CreateIndicatorRequest request) {
        String type = request.type().toUpperCase(Locale.ROOT);
        String value = normalizeValue(type, request.value());
        try {
            return add(new CreateIndicatorRequest(type, value, request.source(), request.confidence(),
                    request.techniqueId(), request.techniqueName(), request.tags()));
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("An indicator with this type and value already exists", exception);
        }
    }

    static String normalizeValue(String type, String rawValue) {
        String value = rawValue.strip();
        boolean valid = switch (type) {
            case "IP" -> IPV4.matcher(value).matches();
            case "DOMAIN" -> DOMAIN.matcher(value).matches();
            case "SHA256" -> SHA256.matcher(value).matches();
            case "EMAIL" -> EMAIL.matcher(value).matches();
            case "URL" -> validUrl(value);
            default -> throw new IllegalArgumentException("Supported indicator types: IP, DOMAIN, URL, SHA256, EMAIL");
        };
        if (!valid) {
            throw new IllegalArgumentException("Value does not match indicator type " + type);
        }
        return switch (type) {
            case "EMAIL", "DOMAIN", "SHA256" -> value.toLowerCase(Locale.ROOT);
            case "URL" -> normalizeUrl(value);
            default -> value;
        };
    }

    private static boolean validUrl(String value) {
        try {
            java.net.URI uri = java.net.URI.create(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getUserInfo() == null && uri.getHost() != null && DOMAIN.matcher(uri.getHost()).matches();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private ThreatIndicator add(CreateIndicatorRequest request) {
        Instant now = Instant.now();
        ThreatIndicator indicator = new ThreatIndicator(UUID.randomUUID(), request.type(), request.value(), request.source(),
                request.confidence(), "ACTIVE", request.techniqueId(), request.techniqueName(), now,
                now.plus(30, ChronoUnit.DAYS), request.tags());
        jdbc.update("INSERT INTO threat_indicators (id, type, value, source, confidence, status, technique_id, "
                        + "technique_name, first_seen, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                indicator.id(), indicator.type(), indicator.value(), indicator.source(), indicator.confidence(),
                indicator.status(), indicator.techniqueId(), indicator.techniqueName(), indicator.firstSeen(), indicator.expiresAt());
        for (int index = 0; index < indicator.tags().size(); index++) {
            jdbc.update("INSERT INTO indicator_tags (indicator_id, tag, position) VALUES (?, ?, ?)",
                    indicator.id(), indicator.tags().get(index), index);
        }
        return indicator;
    }

    private void addMatch(ThreatIndicator indicator, String asset, String context, Instant observedAt) {
        jdbc.update("INSERT INTO threat_matches (id, indicator_id, observable, asset, context, observed_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), indicator.id(), indicator.value(), asset, context, observedAt);
    }

    private static String normalizeUrl(String value) {
        try {
            java.net.URI uri = java.net.URI.create(value);
            int port = uri.getPort();
            if (("http".equalsIgnoreCase(uri.getScheme()) && port == 80)
                    || ("https".equalsIgnoreCase(uri.getScheme()) && port == 443)) {
                port = -1;
            }
            return new java.net.URI(uri.getScheme().toLowerCase(Locale.ROOT), null,
                    uri.getHost().toLowerCase(Locale.ROOT), port, uri.getPath(), uri.getQuery(), null).toASCIIString();
        } catch (java.net.URISyntaxException exception) {
            throw new IllegalArgumentException("URL could not be normalized", exception);
        }
    }

    public record CreateIndicatorRequest(
            @jakarta.validation.constraints.NotBlank String type,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 512) String value,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 120) String source,
            @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(100) int confidence,
            @jakarta.validation.constraints.Size(max = 12) String techniqueId,
            @jakarta.validation.constraints.Size(max = 160) String techniqueName,
            @jakarta.validation.constraints.Size(max = 12) List<@jakarta.validation.constraints.Size(max = 40) String> tags) {
        public CreateIndicatorRequest {
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }
}