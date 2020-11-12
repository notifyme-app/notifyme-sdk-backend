package ch.ubique.notifyme.sdk.backend.model.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Base64;

public class UrlBase64StringDeserializer extends StdDeserializer<byte[]> {

    public UrlBase64StringDeserializer() {
        this(null);
    }

    public UrlBase64StringDeserializer(Class t) {
        super(t);
    }

    @Override
    public byte[] deserialize(JsonParser jsonparser, DeserializationContext context)
            throws IOException {
        String base64 = jsonparser.getValueAsString();
        return Base64.getUrlDecoder().decode(base64.getBytes("UTF-8"));
    }
}
