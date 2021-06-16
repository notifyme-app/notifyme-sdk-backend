package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import java.util.ArrayList;
import java.util.List;

public class FakeRequestFilterTest extends UploadInsertionFilterTest {
    @Override
    List<List<UploadVenueInfo>> getValidVenueInfo() {
        final var venueInfoList = new ArrayList<UploadVenueInfo>();
        final var venueInfo = getVenueInfo(false);
        venueInfoList.add(venueInfo);
        return List.of(venueInfoList);
    }

    @Override
    List<List<UploadVenueInfo>> getInvalidVenueInfo() {
        final var venueInfoList = new ArrayList<UploadVenueInfo>();
        final var venueInfo = getVenueInfo(true);
        venueInfoList.add(venueInfo);
        return List.of(venueInfoList);
    }

    @Override
    UploadInsertionFilter insertionFilter() {
        return new FakeRequestFilter();
    }
}
