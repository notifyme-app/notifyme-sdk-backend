package ch.ubique.notifyme.sdk.backend.model.tracekey;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class TraceKeyUploadPayload {

    @NotNull
    @Valid
    @NotEmpty
    private List<TraceKey> traceKeys;

    public List<TraceKey> getTraceKeys() {
        return traceKeys;
    }

    public void setTraceKeys(List<TraceKey> traceKeys) {
        this.traceKeys = traceKeys;
    }
}
