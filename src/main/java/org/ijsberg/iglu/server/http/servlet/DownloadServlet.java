package org.ijsberg.iglu.server.http.servlet;

import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.http.DownloadSupport;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class DownloadServlet extends HttpServlet {

    abstract String getResourceDir();

    public void service(HttpServletRequest request, HttpServletResponse response) {

        String pathInfo = request.getPathInfo();
        if(pathInfo == null) {
            pathInfo = "";
        }
        String resourcePath = getResourceDir() + '/' + pathInfo;

        try {
            DownloadSupport.downloadFile(response, resourcePath);
        } catch (Exception e) {
            System.out.println(new LogEntry(Level.CRITICAL, "unable to obtain resource " + resourcePath, e));
            response.setStatus(500);
        }
    }

}
