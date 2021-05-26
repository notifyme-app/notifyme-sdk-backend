package ch.ubique.swisscovid.cn.sdk.backend.model.event;

import ch.ubique.swisscovid.cn.sdk.backend.model.VenueTypeOuterClass.VenueType;
import java.util.List;
import javax.validation.constraints.NotNull;

public class CriticalEvent {

    @NotNull private String name;

    @NotNull private String location;

    @NotNull private String room;

    @NotNull private VenueType venueType;

    @NotNull private List<WebDiaryEntry> webDiaryEntries;

    private int caseCount;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(final String room) {
        this.room = room;
    }

    public VenueType getVenueType() {
        return venueType;
    }

    public void setVenueType(final VenueType venueType) {
        this.venueType = venueType;
    }

    public int getCaseCount() {
        return caseCount;
    }

    public void setCaseCount(final int caseCount) {
        this.caseCount = caseCount;
    }

    public List<WebDiaryEntry> getWebDiaryEntries() {
        return webDiaryEntries;
    }

    public void setWebDiaryEntries(final List<WebDiaryEntry> webDiaryEntries) {
        this.webDiaryEntries = webDiaryEntries;
    }
}
