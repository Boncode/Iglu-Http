package org.ijsberg.iglu.util.http;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class HttpSupportTest {

    @Test
    public void testGetParams() {
//        String requestBody = "param1=Blablabla&param2=yeeterskeeeter&param3=bla3asd&extraOption1&extra2";
        String requestBody = "RelayState&someParam=5&TestBooleanParam&SAMLResponse=Ble%%2BPFNpZ25lZEluZm8%asdkjakjdwlbnQ%%%2B";
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
        assertEquals("true",parameterMap.get("RelayState"));
        assertEquals("5",parameterMap.get("someParam"));
        assertEquals("true",parameterMap.get("TestBooleanParam"));
        assertEquals("Ble%%2BPFNpZ25lZEluZm8%asdkjakjdwlbnQ%%%2B",parameterMap.get("SAMLResponse"));
    }
}
