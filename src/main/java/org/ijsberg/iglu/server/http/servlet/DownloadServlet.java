package org.ijsberg.iglu.server.http.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.ResourceException;
import org.ijsberg.iglu.util.http.DownloadSupport;

import java.io.IOException;


public abstract class DownloadServlet extends HttpServlet {

    abstract String getResourceDir();

    public void service(HttpServletRequest request, HttpServletResponse response) {
        String pathInfo = request.getPathInfo();
        if(pathInfo == null) {
            pathInfo = "";
        }
        String resourcePath = getResourceDir() + '/' + pathInfo;
        try {
            DownloadSupport.downloadFile(response,resourcePath);
        } catch (IOException e) {
            System.out.println(new LogEntry(Level.CRITICAL, String.format("failed to download %s", resourcePath), e));
            response.setStatus(500);
        }
    }
}
