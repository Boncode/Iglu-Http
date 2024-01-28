package org.ijsberg.iglu.http.client;


import jakarta.servlet.http.HttpServletRequest;

public class AuthorizationBearer implements HttpHeader {

    private final String token;
    public static final String KEY = "Authorization";
    public static final String VALUE_PREFIX = "Bearer ";

    public AuthorizationBearer(String token) {
        this.token = token;
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getValue() {
        return VALUE_PREFIX + token;
    }

    public String getToken() {
        return token;
    }

    public static AuthorizationBearer getHttpHeader(HttpServletRequest request) {
        String headerValue = request.getHeader(KEY);
        if(headerValue != null && headerValue.startsWith(VALUE_PREFIX)) {
            String token = headerValue.substring(VALUE_PREFIX.length());
            return new AuthorizationBearer(token);
        }
        return null;
    }
}
