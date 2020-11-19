package ch.ubique.notifyme.sdk.backend.ws.controller;

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataService;
import ch.ubique.notifyme.sdk.backend.model.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.util.DateUtil;
import ch.ubique.notifyme.sdk.backend.ws.SodiumWrapper;
import ch.ubique.openapi.docannotations.Documentation;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/v1/debug")
@CrossOrigin(origins = {"https://upload-dev.notify-me.ch", "https://upload.notify-me.ch"})
public class DebugController {
    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    private final NotifyMeDataService dataService;
    private final SodiumWrapper sodiumWrapper;

    public DebugController(NotifyMeDataService dataService, SodiumWrapper sodiumWrapper) {
        this.dataService = dataService;
        this.sodiumWrapper = sodiumWrapper;
    }

    @GetMapping(value = "")
    @Documentation(
            description = "Hello return",
            responses = {"200=>server live"})
    public @ResponseBody ResponseEntity<String> hello() {
        return ResponseEntity.ok()
                .header("X-HELLO", "notifyme")
                .body("Hello from NotifyMe Debug WS v1");
    }

    @PostMapping(value = "/traceKey")
    public @ResponseBody ResponseEntity<String> uploadTraceKey(
            @RequestParam Long startTime, @RequestParam Long endTime, @RequestParam String ctx)
            throws UnsupportedEncodingException {
        TraceKey traceKey = new TraceKey();
        traceKey.setStartTime(DateUtil.toLocalDateTime(startTime));
        traceKey.setEndTime(DateUtil.toLocalDateTime(endTime));
        try {
            traceKey.setSecretKey(
                    sodiumWrapper.decryptQrTrace(
                            Base64.getUrlDecoder().decode(ctx.getBytes("UTF-8"))));
        } catch (InvalidProtocolBufferException e) {
            logger.error("unable to parse decrypted ctx protobuf", e);
        }
        dataService.insertTraceKey(traceKey);
        return ResponseEntity.ok().body("OK");
    }
}
