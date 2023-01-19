package org.ijsberg.iglu.util.http;

import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.access.EventMessage;
import org.ijsberg.iglu.access.component.RequestRegistry;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.ResourceException;
import org.ijsberg.iglu.util.io.FileSupport;
import org.ijsberg.iglu.util.io.StreamSupport;
import org.ijsberg.iglu.util.mail.MimeTypeSupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DownloadSupport {

    private static RequestRegistry requestRegistry;
    public void setRequestRegistry(RequestRegistry requestRegistry) {
        this.requestRegistry = requestRegistry;
    }

    public static File getDownloadableFile(String resourcePath) {
        resourcePath = FileSupport.convertToUnixStylePath(resourcePath);
        if(resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }
        resourcePath = HttpEncodingSupport.urlDecode(resourcePath);
        if(resourcePath.contains("..")) {
            throw new ResourceException("resource path contains suspicious content");
        }

        File downloadable = new File(resourcePath);
        if (!downloadable.exists()) {
            throw new ResourceException("file does not exist");
        }

        return downloadable;
    }

    public static void downloadFile(HttpServletResponse response, String resourcePath) {
        File downloadable = getDownloadableFile(resourcePath);
        String fileName = downloadable.getName();
        System.out.println(new LogEntry(Level.DEBUG, String.format("downloading %s", fileName)));

        response.setContentType(MimeTypeSupport.getMimeTypeForFileName(fileName));
        response.setContentLength((int) downloadable.length());
        response.setHeader("Content-disposition", "attachment; filename=" + fileName);

        try (InputStream input = new FileInputStream(downloadable)) {
            StreamSupport.absorbInputStream(input, response.getOutputStream());
        } catch (IOException e) {
            System.out.println(new LogEntry(Level.CRITICAL, String.format("failed to download %s", fileName), e));
            requestRegistry.dropMessageToCurrentUser(new EventMessage("processFailed", "Download failed with message: " + e.getMessage()));
            response.setStatus(500);
        }
    }
}
