package ch.ubique.notifyme.sdk.backend.ws.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DateTimeUtil {

  /**
   * To avoid timing attacks where the duration of the API is used to infer what the user requested,
   * all requests that change the database call this method to have the same duration, so an outside
   * attacker cannot infer anything on the response time. If the caller already spent too much time,
   * an exception is thrown.
   *
   * @param start time the api request started
   * @param totalDuration static duration of each api request
   * @throws InterruptedException sleep interrupted by another thread
   * @throws DurationExpiredException caller already spent too much time on the request
   */
  public static void normalizeDuration(LocalDateTime start, Duration totalDuration)
      throws InterruptedException, DurationExpiredException {
    Duration timeFillUp =
        totalDuration.minus(Duration.ofMillis(start.until(LocalDateTime.now(), ChronoUnit.MILLIS)));
    if (timeFillUp.isNegative()) {
      throw new DurationExpiredException("Duration of call was longer than requestDuration");
    } else {
      Thread.sleep(timeFillUp.toMillis());
    }
  }

  /** Indicates that the requested maximum duration already expired. */
  public static class DurationExpiredException extends Exception {
    public DurationExpiredException(String errorMessage) {
      super(errorMessage);
    }
  }
}
