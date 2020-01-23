package org.ijsberg.iglu.server.http.servlet;

import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.http.HttpEncodingSupport;
import org.ijsberg.iglu.util.io.FileSupport;
import org.ijsberg.iglu.util.io.StreamSupport;
import org.ijsberg.iglu.util.mail.MimeTypeSupport;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.InputStream;

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
