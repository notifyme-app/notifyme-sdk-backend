package ch.ubique.notifyme.sdk.backend.ws.security;

import ch.ubique.notifyme.sdk.backend.model.tracekey.v3.TraceKey;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.format.DateTimeFormatter;

public class NotifyMeJwtRequestValidator implements RequestValidator {

    @Override
    public boolean isValid(Object authObject) throws WrongScopeException, WrongAudienceException, NotAJwtException {
        if(authObject instanceof Jwt) {
            Jwt token = (Jwt) authObject;
            if (Boolean.TRUE.equals(token.containsClaim("scope")) && token.getClaim("scope").equals("userupload")) {
                if(token.getAudience().contains("checkin")) {
                    return true;
                } else {
                    throw new WrongAudienceException();
                }
            } else {
                throw new WrongScopeException();
            }
        }
        throw new NotAJwtException();
    }

    @Override
    public long validateKeyDate(Object authObject, Object others) throws ClaimIsBeforeOnsetException, InvalidDateException {
    // TODO Implement: Check not larger than threshold (~24h), onset before start of interval (for each interval)
    final var DATE_FORMATTER = DateTimeFormatter.ofPattern("YYYY-MM-dd"); // parse as local date
    if (authObject instanceof Jwt) {
      Jwt token = (Jwt) authObject;
//      var jwtKeyDate = DateTime.parseDate(token.getClaim("onset"));
      if(others instanceof TraceKey) {
          final var key = (TraceKey) others;
      }
    }
    throw new IllegalArgumentException();
    }

    @Override
    public boolean isFakeRequest(Object authObject, Object others) {
        // TODO Implement
        return false;
    }
}
