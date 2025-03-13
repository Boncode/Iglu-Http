package org.ijsberg.iglu.util.http;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
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
}
