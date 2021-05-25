package ch.ubique.notifyme.sdk.backend.ws.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotifyMeJwtRequestValidator implements RequestValidator {

    @Override
    public boolean isValid(Object authObject) throws WrongScopeException, WrongAudienceException, NotAJwtException, InvalidOnsetException {
        if(authObject instanceof Jwt) {
            Jwt token = (Jwt) authObject;
            if (Boolean.TRUE.equals(token.containsClaim("scope")) && token.getClaim("scope").equals("userupload")) {
                if(token.getAudience().contains("checkin")) {
                    if(isOnsetBefore(authObject, LocalDateTime.now())) {
                        return true;
                    } else {
                        throw new InvalidOnsetException();
                    }
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
    public boolean isOnsetBefore(Object authObject, LocalDateTime dateTime) {
        if(authObject instanceof Jwt) {
            Jwt token = (Jwt) authObject;
            final var dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            final var onset = LocalDate.parse(token.getClaim("onset"), dateTimeFormatter);
            return !onset.isAfter(dateTime.toLocalDate()); // Use isAfter because onset could be on the same day
        }
        return false;
    }


    @Override
    public boolean isFakeRequest(Object authObject, Object others) {
        if (authObject instanceof Jwt) {
            Jwt token = (Jwt) authObject;
            return Boolean.TRUE.equals(token.containsClaim("fake")) && "1".equals(token.getClaim("fake"));
        }
        return false;
    }
}
