package org.ijsberg.iglu.server.http.servlet;

import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.http.DownloadSupport;
import org.ijsberg.iglu.util.io.StreamSupport;
import org.ijsberg.iglu.util.mail.MimeTypeSupport;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class DownloadServlet extends HttpServlet {

    abstract String getResourceDir();

    public void service(HttpServletRequest request, HttpServletResponse response) {

        String pathInfo = request.getPathInfo();
        if(pathInfo == null) {
            pathInfo = "";
        }
        String resourcePath = getResourceDir() + '/' + pathInfo;

        File downloadable = DownloadSupport.getDownloadableFile(resourcePath);

        String fileName = downloadable.getName();
        response.setContentType(MimeTypeSupport.getMimeTypeForFileExtension(fileName.substring(fileName.lastIndexOf('.') + 1)));
        response.setContentLength((int) downloadable.length());
        response.setHeader("Content-disposition", "attachment; filename=" + fileName);

        try (InputStream input = new FileInputStream(resourcePath)) {
            StreamSupport.absorbInputStream(input, response.getOutputStream());
        } catch (IOException e) {
            System.out.println(new LogEntry(Level.CRITICAL, String.format("failed to download %s", resourcePath), e));
            response.setStatus(500);
        }

    }

}
