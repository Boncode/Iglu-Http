package org.ijsberg.iglu.server.http.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

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
