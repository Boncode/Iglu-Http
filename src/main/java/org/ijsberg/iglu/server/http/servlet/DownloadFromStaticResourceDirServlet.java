package org.ijsberg.iglu.server.http.servlet;


import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;

public class DownloadFromStaticResourceDirServlet extends DownloadServlet {

    protected String resourceDir;

    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);
        resourceDir = conf.getInitParameter("resource_dir");
        if(resourceDir == null) {
            resourceDir = "";
        }
    }

    @Override
    String getResourceDir() {
        return resourceDir;
    }
}
