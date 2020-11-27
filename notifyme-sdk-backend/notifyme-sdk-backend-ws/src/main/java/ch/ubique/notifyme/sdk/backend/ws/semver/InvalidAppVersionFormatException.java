package ch.ubique.notifyme.sdk.backend.ws.semver;

public class InvalidAppVersionFormatException extends RuntimeException {

    public InvalidAppVersionFormatException() {
        super("invalid app-version format");
    }
}
