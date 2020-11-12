package ch.ubique.notifyme.sdk.backend.ws.controller;

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataService;
import ch.ubique.notifyme.sdk.backend.model.ProblematicEventWrapperOuterClass.ProblematicEvent;
import ch.ubique.notifyme.sdk.backend.model.ProblematicEventWrapperOuterClass.ProblematicEventWrapper;
import ch.ubique.notifyme.sdk.backend.model.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.util.DateUtil;
import ch.ubique.openapi.docannotations.Documentation;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/v1")
public class NotifyMeController {
    private final NotifyMeDataService dataService;
    private final String revision;

    public NotifyMeController(NotifyMeDataService dataService, String revision) {
        this.dataService = dataService;
        this.revision = revision;
    }

    @GetMapping(value = "")
    @Documentation(
            description = "Hello return",
            responses = {"200=>server live"})
    public @ResponseBody ResponseEntity<String> hello() {
        return ResponseEntity.ok().header("X-HELLO", "notifyme").body("Hello from NotifyMe WS v1.\n" + revision);
    }

    @GetMapping(
            value = "/traceKeys",
            produces = {"application/json"})
    public @ResponseBody ResponseEntity<List<TraceKey>> getTraceKeysJson(
            @RequestParam(required = false) Long lastSync) {
        return ResponseEntity.ok()
                .body(dataService.findTraceKeys(DateUtil.toLocalDateTime(lastSync)));
    }

    @GetMapping(
            value = "/traceKeys",
            produces = {"application/protobuf"})
    public @ResponseBody ResponseEntity<byte[]> getTraceKeys(
            @RequestParam(required = false) Long lastSync) {
        List<TraceKey> traceKeys = dataService.findTraceKeys(DateUtil.toLocalDateTime(lastSync));
        ProblematicEventWrapper pew =
                ProblematicEventWrapper.newBuilder()
                        .setVersion(1)
                        .addAllEvents(mapToProblematicEvents(traceKeys))
                        .build();
        return ResponseEntity.ok().body(pew.toByteArray());
    }

    private List<ProblematicEvent> mapToProblematicEvents(List<TraceKey> traceKeys) {
        return traceKeys.stream()
                .map(
                        t ->
                                ProblematicEvent.newBuilder()
                                        .setSecretKey(ByteString.copyFrom(t.getSecretKey()))
                                        .setStartTime(DateUtil.toEpochMilli(t.getStartTime()))
                                        .setEndTime(DateUtil.toEpochMilli(t.getEndTime()))
                                        .build())
                .collect(Collectors.toList());
    }
}
