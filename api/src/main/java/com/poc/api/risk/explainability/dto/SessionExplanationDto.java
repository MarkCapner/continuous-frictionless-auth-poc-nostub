
package com.poc.api.risk.explainability.dto;

import java.util.List;

public record SessionExplanationDto(
        String sessionId,
        List<DomainExplanationDto> domains
) {}
