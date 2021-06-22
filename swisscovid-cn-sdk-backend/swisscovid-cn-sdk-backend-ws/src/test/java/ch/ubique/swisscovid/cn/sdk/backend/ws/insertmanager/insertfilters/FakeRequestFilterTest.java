package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import java.util.ArrayList;
import java.util.List;

public class FakeRequestFilterTest extends UploadInsertionFilterTest {
    @Override
    List<List<UploadVenueInfo>> getValidVenueInfo() {
        final var venueInfo = getVenueInfo(false);
        final var venueInfoList = new ArrayList<>(venueInfo);
        return List.of(venueInfoList);
    }

    @Override
    List<List<UploadVenueInfo>> getInvalidVenueInfo() {
        final var venueInfo = getVenueInfo(true);
        final var venueInfoList = new ArrayList<>(venueInfo);
        return List.of(venueInfoList);
    }

    @Override
    UploadInsertionFilter insertionFilter() {
        return new FakeRequestFilter();
    }
}
