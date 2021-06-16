package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import java.time.LocalDateTime;
import java.util.List;

public class FutureVenueInfoFilterTest extends UploadInsertionFilterTest {

    final LocalDateTime now = LocalDateTime.now();

    @Override
    List<List<UploadVenueInfo>> getValidVenueInfo() {
        return List.of(List.of((getVenueInfo(now.minusHours(2), now.minusHours(1)))));
    }

    @Override
    List<List<UploadVenueInfo>> getInvalidVenueInfo() {
        return List.of(List.of(getVenueInfo(now, now.plusHours(1))));
    }

    @Override
    UploadInsertionFilter insertionFilter() {
        return new FutureVenueInfoFilter();
    }
}
