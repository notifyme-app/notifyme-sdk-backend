package ch.ubique.n2step.sdk.backend.model;

import ch.ubique.n2step.sdk.backend.model.util.LocalDateTimeDeserializer;
import ch.ubique.n2step.sdk.backend.model.util.LocalDateTimeSerializer;
import ch.ubique.n2step.sdk.backend.model.util.UrlBase64StringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import javax.validation.constraints.NotNull;

public class TraceKeyUpload {
    @JsonDeserialize(using = UrlBase64StringDeserializer.class)
    @NotNull
    private byte[] ctx;

    @NotNull
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime startTime;

    @NotNull
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    public byte[] getCtx() {
        return ctx;
    }

    public void setCtx(byte[] ctx) {
        this.ctx = ctx;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
