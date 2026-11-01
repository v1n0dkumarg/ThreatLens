package dev.portfolio.threatlens;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class IndicatorController {
    private final IndicatorService service;

    public IndicatorController(IndicatorService service) {
        this.service = service;
    }

    @GetMapping("/indicators")
    public List<ThreatIndicator> indicators() {
        return service.indicators();
    }

    @GetMapping("/matches")
    public List<ThreatMatch> matches() {
        return service.matches();
    }

    @PostMapping("/indicators")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreatIndicator create(@Valid @RequestBody IndicatorService.CreateIndicatorRequest request) {
        try {
            return service.create(request);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
}