package ch.ubique.notifyme.sdk.backend.model.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.Base64;

public class UrlBase64StringSerializer extends StdSerializer<byte[]> {

    public UrlBase64StringSerializer() {
        this(null);
    }

    public UrlBase64StringSerializer(Class t) {
        super(t);
    }

    @Override
    public void serialize(byte[] value, JsonGenerator gen, SerializerProvider arg2)
            throws IOException, JsonProcessingException {
        gen.writeString(new String(Base64.getUrlEncoder().encode(value)));
    }
}
