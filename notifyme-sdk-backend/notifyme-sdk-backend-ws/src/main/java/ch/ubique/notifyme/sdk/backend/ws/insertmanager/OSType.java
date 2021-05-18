package ch.ubique.notifyme.sdk.backend.ws.insertmanager;

public enum OSType {
    ANDROID,
    IOS;

    @Override
    public String toString() {
        switch (this) {
            case ANDROID:
                return "Android";
            case IOS:
                return "iOS";
            default:
                return "Unknown";
        }
    }
}
