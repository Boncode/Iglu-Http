package org.ijsberg.iglu.server.http.servlet;

import org.ijsberg.iglu.access.component.RequestRegistry;
import org.ijsberg.iglu.util.http.ServletSupport;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

public class DownloadFromUserSpecificResourceDirServlet extends DownloadServlet {

    private RequestRegistry requestRegistry;
    private String downloadSubDir = "downloads";
    private String uploadRootDir = "uploads";

    public void setRequestRegistry(RequestRegistry requestRegistry) {
        this.requestRegistry = requestRegistry;
    }

    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);
        if(conf.getInitParameter("download_sub_dir") != null) {
            downloadSubDir = conf.getInitParameter("download_sub_dir");
        }
        if(conf.getInitParameter("upload_root_dir") != null) {
            downloadSubDir = conf.getInitParameter("upload_root_dir");
        }
    }


    @Override
    String getResourceDir() {
        return uploadRootDir + "/" + ServletSupport.getUserDir(requestRegistry) + "/" + downloadSubDir;
    }
}
