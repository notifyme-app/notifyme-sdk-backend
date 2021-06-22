package ch.ubique.swisscovid.cn.sdk.backend.data;

import java.time.Duration;
import java.time.LocalDateTime;

public interface KPIDataService {

    /**
     * Insert the number of checkins in a particular upload
     *
     * @param uploadDate Time of upload - will be truncated to date
     * @param count Number of checkins included in upload
     */
    public void insertCheckinCount(LocalDateTime uploadDate, int count);

    /**
     * Delete all checkin counts strictly before a certain date
     *
     * @param retentionPeriod Time before which all checkin counts should be deleted - Will be truncated to
     *     day
     * @return
     */
    public int cleanDB(Duration retentionPeriod);
}
