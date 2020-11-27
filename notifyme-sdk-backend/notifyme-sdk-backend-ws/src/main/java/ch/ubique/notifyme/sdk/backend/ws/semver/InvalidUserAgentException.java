package ch.ubique.notifyme.sdk.backend.ws.semver;

public class InvalidUserAgentException extends RuntimeException {

    public InvalidUserAgentException() {
        super("invalid user-agent");
    }
}
