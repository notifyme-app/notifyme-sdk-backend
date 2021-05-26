package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class FutureVenueInfoFilterTest extends UploadInsertionFilterTest {

    final LocalDateTime now = LocalDateTime.now();

    @Override
    List<UploadVenueInfo> getValidVenueInfo() {
        return Collections.singletonList(getVenueInfo(now.minusHours(2), now.minusHours(1)));
    }

    @Override
    List<UploadVenueInfo> getInvalidVenueInfo() {
        return Collections.singletonList(getVenueInfo(now, now.plusHours(1)));
    }

    @Override
    UploadInsertionFilter insertionFilter() {
        return new FutureVenueInfoFilter();
    }
}
