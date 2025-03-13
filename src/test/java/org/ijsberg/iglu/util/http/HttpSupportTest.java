package org.ijsberg.iglu.util.http;

import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class HttpSupportTest {

    @Test
    public void testGetParams() {
//        String requestBody = "param1=Blablabla&param2=yeeterskeeeter&param3=bla3asd&extraOption1&extra2";
        String requestBody = "RelayState=&someParam=5&TestBooleanParam&SAMLResponse=Ble%%2BPFNpZ25lZEluZm8%asdkjakjdwlbnQ%%%2B";
        Map<String, String> paramsMap = new HashMap<>();
        System.out.println(new LogEntry(Level.DEBUG, "Body: " + requestBody));
        String[] params = requestBody.split("&");
        for(String param : params) {
            boolean isKeyValuePair = param.contains("=");
            if (isKeyValuePair) {
                String key = param.split("=")[0];
                String value = param.split("=").length == 2 ? param.split("=")[1] : "";
                System.out.println(new LogEntry(Level.DEBUG, "Found param for SAML Response: " + key + " = " + value));
                paramsMap.put(key, value);
            } else {
                String key = param;
                String value = "true";
                System.out.println(new LogEntry(Level.DEBUG, "Found param for SAML Response: " + key + " = " + value));
                paramsMap.put(key, value);
            }
        }
    }
}
