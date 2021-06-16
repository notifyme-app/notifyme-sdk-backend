package ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.insertfilters;

import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass;
import ch.ubique.swisscovid.cn.sdk.backend.model.UserUploadPayloadOuterClass.UploadVenueInfo;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.InsertException;
import ch.ubique.swisscovid.cn.sdk.backend.ws.insertmanager.OverlappingIntervalsException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A person can't be in two places at the same time. The filter checks the VenueInfo objects for
 * integrity.
 *
 * <ol>
 *   <li>The VenueInfo objects are iterated in the exact order in which they arrived in the request,
 *       concatenating their time intervals until the preid changes or the time intervals aren't
 *       continuous
 *   <li>30min are subtracted from the resulting interval to account for those added upon upload
 *   <li>The interval is added to a list
 *   <li>Finally, once all VenueInfo objects have been processed, the list is checked for
 *       inconsistencies, i.e. overlapping intervals
 * </ol>
 */
public class OverlappingIntervalsFilter implements UploadInsertionFilter {
    @Override
    public List<UserUploadPayloadOuterClass.UploadVenueInfo> filter(
            LocalDateTime now, List<UploadVenueInfo> uploadVenueInfoList, Object principal)
            throws InsertException {
        if (!uploadVenueInfoList.isEmpty()) {
            List<TimeInterval> timeIntervals = getTimeIntervalList(uploadVenueInfoList);
            for (var i = 0; i < timeIntervals.size() - 1; i++) {
                final var t1 = timeIntervals.get(i);
                for (var j = i + 1; j < timeIntervals.size(); j++) {
                    if (TimeInterval.doIntersect(t1, timeIntervals.get(j))) {
                        throw new OverlappingIntervalsException();
                    }
                }
            }
        }
        return uploadVenueInfoList;
    }

    private List<TimeInterval> getTimeIntervalList(List<UploadVenueInfo> uploadVenueInfoList) {
        List<TimeInterval> timeIntervals = new ArrayList<>();
        var timeInterval =
                new TimeInterval(
                        uploadVenueInfoList.get(0).getIntervalStartMs(),
                        uploadVenueInfoList.get(0).getIntervalStartMs());
        for (var i = 0; i < uploadVenueInfoList.size(); i++) {
            final var venueInfo = uploadVenueInfoList.get(i);
            timeInterval.setIntervalEndMs(venueInfo.getIntervalEndMs());
            if (i == uploadVenueInfoList.size() - 1) {
                timeInterval.setIntervalEndMs(timeInterval.getIntervalEndMs() - 30 * 60 * 1000L);
                timeIntervals.add(timeInterval);
            } else {
                final var nextVenueInfo = uploadVenueInfoList.get(i + 1);
                if (!venueInfo.getPreId().equals(nextVenueInfo.getPreId())
                        || venueInfo.getIntervalEndMs() != nextVenueInfo.getIntervalStartMs()) {
                    timeInterval.setIntervalEndMs(
                            timeInterval.getIntervalEndMs() - 30 * 60 * 1000L);
                    timeIntervals.add(timeInterval);
                    timeInterval =
                            new TimeInterval(
                                    nextVenueInfo.getIntervalStartMs(),
                                    nextVenueInfo.getIntervalStartMs());
                }
            }
        }
        return timeIntervals;
    }

    private static class TimeInterval {
        private final long intervalStartMs;
        private long intervalEndMs;

        public TimeInterval(long intervalStartMs, long intervalEndMs) {
            this.intervalStartMs = intervalStartMs;
            this.intervalEndMs = intervalEndMs;
        }

        /** Checks whether two given time intervals overlap */
        public static boolean doIntersect(TimeInterval t1, TimeInterval t2) {
            return !(t1.getIntervalEndMs() <= t2.getIntervalStartMs()
                    || t2.getIntervalEndMs() <= t1.getIntervalStartMs());
        }

        public long getIntervalStartMs() {
            return intervalStartMs;
        }

        public long getIntervalEndMs() {
            return intervalEndMs;
        }

        public void setIntervalEndMs(long intervalEndMs) {
            this.intervalEndMs = intervalEndMs;
        }
    }
}
