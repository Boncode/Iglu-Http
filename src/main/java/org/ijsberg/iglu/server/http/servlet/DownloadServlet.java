package org.ijsberg.iglu.server.http.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.util.http.DownloadSupport;


public abstract class DownloadServlet extends HttpServlet {

    abstract String getResourceDir();

    public void service(HttpServletRequest request, HttpServletResponse response) {
        String pathInfo = request.getPathInfo();
        if(pathInfo == null) {
            pathInfo = "";
        }
        String resourcePath = getResourceDir() + '/' + pathInfo;
        DownloadSupport.downloadFile(response,resourcePath);
    }
}
