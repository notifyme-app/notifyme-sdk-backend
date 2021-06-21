package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass;
import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.InsertException;

import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.InvalidFormatException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * To prevent the possibility of traffic analysis, fake requests are sent at semi-regular intervals.
 * Fake uploads don't actually need to be inserted into the database and can therefore be dropped.
 */
public class FakeRequestFilter implements UploadInsertionFilter {
  @Override
  public List<UserUploadPayloadOuterClass.UploadVenueInfo> filter(
      LocalDateTime now,
      List<UploadVenueInfo> uploadVenueInfoList,
      Object principal)
      throws InsertException {
    for (UploadVenueInfo uploadVenueInfo : uploadVenueInfoList) {
      if (uploadVenueInfo.getFake().toByteArray().length != 1) {
        throw new InvalidFormatException();
      }
    }
    return uploadVenueInfoList.stream()
        .filter(uploadVenueInfo -> (uploadVenueInfo.getFake().toByteArray()[0] == 0)) // only collect real venue infos.
        .collect(Collectors.toList());
  }
}
