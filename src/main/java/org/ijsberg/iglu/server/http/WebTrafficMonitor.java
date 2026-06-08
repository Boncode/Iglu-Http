package org.ijsberg.iglu.server.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface WebTrafficMonitor {
    boolean allowRequest(HttpServletRequest req, HttpServletResponse resp);
    void checkResponse(HttpServletRequest req, HttpServletResponse resp);
}
