package ch.ubique.n2step.sdk.backend.ws.controller;

import ch.ubique.n2step.sdk.backend.data.N2StepDataService;
import ch.ubique.n2step.sdk.backend.model.TraceKey;
import ch.ubique.n2step.sdk.backend.model.util.DateUtil;
import ch.ubique.openapi.docannotations.Documentation;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/v1")
public class N2StepController {
    private final N2StepDataService dataService;

    public N2StepController(N2StepDataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping(value = "")
    @Documentation(
            description = "Hello return",
            responses = {"200=>server live"})
    public @ResponseBody ResponseEntity<String> hello() {
        return ResponseEntity.ok().header("X-HELLO", "n2step").body("Hello from N2STEP WS v1");
    }

    @GetMapping(value = "/traceKeys")
    public @ResponseBody ResponseEntity<List<TraceKey>> getTraceKeys(
            @RequestParam(required = false) Long lastSync) {
        return ResponseEntity.ok()
                .body(dataService.findTraceKeys(DateUtil.toLocalDateTime(lastSync)));
    }
}
