package org.ijsberg.iglu.util.http;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpSupport {

    public static Map<String, String> getRequestBodyParams(HttpServletRequest request) throws IOException {
        String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        Map<String, String> parameterMap = new HashMap<>();
        String[] requestBodyParameters = requestBody.split("&");
        for (String requestBodyParameterString : requestBodyParameters) {
            boolean isKeyValuePair = requestBodyParameterString.split("=").length == 2;
            if (isKeyValuePair) {
                String[] keyValuePair = requestBodyParameterString.split("=");
                parameterMap.put(keyValuePair[0], keyValuePair[1]);
            } else {
                parameterMap.put(requestBodyParameterString, "true");
            }
        }
        return parameterMap;
    }

    /**
     * Retrieves the client IP address for a given HttpServletRequest.
     * First tries the X-Forwarded-For header. If that holds no value it falls back on the remote address. If the
     * X-Forwarded-For header contains multiple IPs (caused by load-balancers and/or proxies), we take the left-most
     * value, which is usually the client IP.
     * @param servletRequest
     * @return the client IP address as a string
     */
    public static String getClientIpAddress(HttpServletRequest servletRequest) {
        String xForwardedFor = servletRequest.getHeader("X-Forwarded-For");
        if(xForwardedFor == null) {
            return servletRequest.getRemoteAddr();
        }

        // could be multiple forward/reverse proxies, client ip is first in the list
        if(xForwardedFor.contains(",")) {
            return xForwardedFor.split(",")[0];
        }
        return xForwardedFor;
    }

    public static boolean isPortAvailable(int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return false;
        } catch (IOException ignored) {
            return true;
        }
    }
}
