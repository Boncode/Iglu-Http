package org.ijsberg.iglu.server.http.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.http.DownloadSupport;
import org.ijsberg.iglu.util.io.StreamSupport;
import org.ijsberg.iglu.util.mail.MimeTypeSupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class DownloadServlet extends HttpServlet {

    abstract String getResourceDir();

    public void service(HttpServletRequest request, HttpServletResponse response) {

        File downloadable = getDownloadable(request);

        String fileName = downloadable.getName();
        response.setContentType(MimeTypeSupport.getMimeTypeForFileExtension(fileName.substring(fileName.lastIndexOf('.') + 1)));
        response.setContentLength((int) downloadable.length());
        response.setHeader("Content-disposition", "attachment; filename=" + fileName);

        try (InputStream input = new FileInputStream(downloadable)) {
            StreamSupport.absorbInputStream(input, response.getOutputStream());
        } catch (IOException e) {
            System.out.println(new LogEntry(Level.CRITICAL, String.format("failed to download %s", downloadable.getName()), e));
            response.setStatus(500);
        }
/*        catch (RuntimeException re) {

        }
*/
    }

    protected File getDownloadable(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if(pathInfo == null) {
            pathInfo = "";
        }
        String resourcePath = getResourceDir() + '/' + pathInfo;
        File downloadable = DownloadSupport.getDownloadableFile(resourcePath);
        return downloadable;
    }

}
