package org.ijsberg.iglu.util.http;

import org.ijsberg.iglu.http.json.JsonData;
import org.ijsberg.iglu.util.misc.StringSupport;

/**
 * Created by jeroe on 24/01/2018.
 */
public abstract class RestSupport {
    private RestSupport(){};

    public static String getExceptionMessage(Throwable e) {
        StringBuffer result = new StringBuffer();
        while (e != null) {
            result.append("\n" + StringSupport.getStackTrace(e, 30) + "\n");
            e = e.getCause();
        }
        return result.toString();
    }

    public static JsonData createResponse(int status, String message) {
        JsonData jsonData = new JsonData();
        jsonData.addAttribute("status", status);
        jsonData.addStringAttribute("message", message);
        //jsonData.addStringAttribute("data", new JsonArray().toString());
        return jsonData;
    }

    public static JsonData createResponse(int status, String message, Exception e) {
        return createResponse(status, message/*, getExceptionMessage(e)*/);
    }

    public static JsonData createResponse(int status, String message, String details) {
        JsonData jsonData = createResponse(status, message);
        jsonData.addStringAttribute("details", details);
        return jsonData;
    }
}
