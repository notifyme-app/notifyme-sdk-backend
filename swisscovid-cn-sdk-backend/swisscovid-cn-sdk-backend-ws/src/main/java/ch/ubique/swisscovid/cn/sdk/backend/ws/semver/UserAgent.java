package ch.ubique.swisscovid.cn.sdk.backend.ws.semver;

public class UserAgent {

    private String appId;
    private Version appVersion;
    private String os;
    private String osVersion;

    public UserAgent(String userAgent) {
        String[] split = userAgent.split(";");
        if (split.length != 4) {
            throw new InvalidUserAgentException();
        }
        this.appId = split[0];
        this.appVersion = new Version(split[1]);
        if (!this.appVersion.isValid()) {
            throw new InvalidAppVersionFormatException();
        }
        this.os = split[2];
        this.osVersion = split[3];
    }

    public String getAppId() {
        return appId;
    }

    public Version getAppVersion() {
        return appVersion;
    }

    public String getOs() {
        return os;
    }

    public String getOsVersion() {
        return osVersion;
    }
}
