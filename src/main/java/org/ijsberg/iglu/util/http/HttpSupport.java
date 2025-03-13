package org.ijsberg.iglu.util.http;

import jakarta.servlet.http.HttpServletRequest;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpSupport {

    public static Map<String, String> getRequestBodyParams(HttpServletRequest request) throws IOException {
        String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        Map<String, String> paramsMap = new HashMap<>();
        System.out.println(new LogEntry(Level.DEBUG, "SAML Post body: " + requestBody));
        String[] params = requestBody.split("&");
        for(String param : params) {
            boolean isKeyValuePair = param.split("=").length == 2;
            if(isKeyValuePair) {
                String key = param.split("=")[0];
                String value = param.split("=")[1];
                System.out.println(new LogEntry(Level.DEBUG, "Found param for SAML Response: " + key + " = " + value));
                paramsMap.put(key, value);
            } else {
                String key = param;
                String value = "true";
                System.out.println(new LogEntry(Level.DEBUG, "Found param for SAML Response: " + key + " = " + value));
                paramsMap.put(key, value);
            }
        }
        return paramsMap;
    }
}
