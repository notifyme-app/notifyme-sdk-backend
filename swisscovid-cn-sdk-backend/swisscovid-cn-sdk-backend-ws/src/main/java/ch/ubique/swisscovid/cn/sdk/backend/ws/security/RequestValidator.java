package ch.ubique.swisscovid.cn.sdk.backend.ws.security;

import java.time.LocalDateTime;

public interface RequestValidator {
  /**
   * Checks if the authObject contains the correct scope and audience
   *
   * @throws WrongScopeException, WrongAudienceException
   */
  public boolean isValid(Object authObject)
      throws WrongScopeException, WrongAudienceException, NotAJwtException, InvalidOnsetException;

  /**
   * Checks if the date in the onset claim is before the given date
   *
   * @param authObject JWT containing the onset claim
   * @param dateTime Date to check
   * @return true if onset is before dateTime, false otherwise
   */
  public boolean isOnsetBefore(Object authObject, LocalDateTime dateTime);

  /**
   * Checks if the request is fake by checking both the token's fake claim and the request's "fake"
   * field
   *
   * @param authObject JWT containing the fake claim
   * @param others The request
   * @return boolean indicating whether or not the request is fake
   */
  public boolean isFakeRequest(Object authObject, Object others);

  public class InvalidOnsetException extends Exception {
    private static final long serialVersionUID = 5886601055826066148L;
  }

  public class ClaimDoesNotMatchKeyDateException extends Exception {
    private static final long serialVersionUID = 5886601055826066149L;
  }

  public class ClaimIsBeforeOnsetException extends Exception {
    private static final long serialVersionUID = 5886601055826066150L;
  }

  public class WrongScopeException extends Exception {
    private static final long serialVersionUID = 5886601055826066151L;
  }

  public class WrongAudienceException extends Exception {
    private static final long serialVersionUID = 5886601055826066152L;
  }

  public class NotAJwtException extends Exception {
    private static final long serialVersionUID = 5886601055826066153L;
  }
}
