package ch.ubique.notifyme.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.notifyme.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class FakeRequestFilterTest extends UploadInsertionFilterTest {
  @Override
  List<UploadVenueInfo> getValidVenueInfo() {
    final var venueInfoList = new ArrayList<UploadVenueInfo>();
    final var venueInfo = getVenueInfo(false);
    venueInfoList.add(venueInfo);
    return venueInfoList;
  }

  @Override
  List<UploadVenueInfo> getInvalidVenueInfo() {
    final var venueInfoList = new ArrayList<UploadVenueInfo>();
    final var venueInfo = getVenueInfo(true);
    venueInfoList.add(venueInfo);
    return venueInfoList;
  }

  @Override
  UploadInsertionFilter insertionFilter() {
    return new FakeRequestFilter();
  }
}
