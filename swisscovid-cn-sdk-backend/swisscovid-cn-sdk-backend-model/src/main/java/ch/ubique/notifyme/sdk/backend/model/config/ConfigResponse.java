package ch.ubique.notifyme.sdk.backend.model.config;

import ch.ubique.openapi.docannotations.Documentation;

public class ConfigResponse {
    @Documentation(
            description =
                    "Blocks the app and shows a link to the app-store. The user can only continue once the app is updated")
    private boolean forceUpdate = false;

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }
}
