package ch.ubique.n2step.sdk.backend.ws.controller;

import ch.ubique.n2step.sdk.backend.data.N2StepDataService;
import ch.ubique.n2step.sdk.backend.model.TraceKey;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/v1/debug")
public class DebugController {
    private final N2StepDataService dataService;

    public DebugController(N2StepDataService dataService) {
        this.dataService = dataService;
    }

    public @ResponseBody ResponseEntity<String> hello() {
        return ResponseEntity.ok()
                .header("X-HELLO", "n2step")
                .body("Hello from N2STEP Debug WS v1");
    }

    @PostMapping(value = "/traceKey")
    public @ResponseBody ResponseEntity<String> uploadTraceKey(
            @RequestBody(required = false) TraceKey traceKey) {
        dataService.insertTraceKey(traceKey);
        return ResponseEntity.ok().body("OK");
    }
}
