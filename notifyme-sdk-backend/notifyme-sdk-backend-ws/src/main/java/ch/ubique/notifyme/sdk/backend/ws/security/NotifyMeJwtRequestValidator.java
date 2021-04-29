package ch.ubique.notifyme.sdk.backend.ws.security;

import org.springframework.security.oauth2.jwt.Jwt;

public class NotifyMeJwtRequestValidator implements RequestValidator {

    @Override
    public boolean isValid(Object authObject) throws WrongScopeException, WrongAudienceException, NotAJwtException {
        if(authObject instanceof Jwt) {
            Jwt token = (Jwt) authObject;
            if (Boolean.TRUE.equals(token.containsClaim("scope")) && token.getClaim("scope").equals("userupload")) {
                if(token.getAudience().contains("notifyMe")) {
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
        // TODO Implement
        return 0;
    }

    @Override
    public boolean isFakeRequest(Object authObject, Object others) {
        // TODO Implement
        return false;
    }
}
