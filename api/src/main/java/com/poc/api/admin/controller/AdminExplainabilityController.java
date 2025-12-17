
package com.poc.api.admin.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;
import com.poc.api.risk.explainability.dto.*;
import com.poc.api.risk.explainability.service.SessionExplanationService;

@RestController
@RequestMapping("/api/admin/explainability")
public class AdminExplainabilityController {

    private final SessionExplanationService service = new SessionExplanationService();

    @GetMapping("/session/{id}")
    public SessionExplanationDto explainSession(@PathVariable String id) {
        return service.explain(id, List.of());
    }
}
