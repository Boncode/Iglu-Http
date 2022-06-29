package org.ijsberg.iglu.util.http;

import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.ResourceException;
import org.ijsberg.iglu.util.io.FileSupport;
import org.ijsberg.iglu.util.io.StreamSupport;
import org.ijsberg.iglu.util.mail.MimeTypeSupport;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DownloadSupport {

    public static void downloadFile(HttpServletResponse response, String resourcePath) throws IOException {
        resourcePath = FileSupport.convertToUnixStylePath(resourcePath);
        if(resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }
        resourcePath = HttpEncodingSupport.urlDecode(resourcePath);

        if(resourcePath.contains("..")) {
            throw new ResourceException("resource path contains suspicious content");
        }

        File downloadable = new File(resourcePath);
        System.out.println(new LogEntry(Level.DEBUG, String.format("downloading %s", resourcePath)));

        response.setContentType(MimeTypeSupport.getMimeTypeForFileExtension(resourcePath.substring(resourcePath.lastIndexOf('.') + 1)));
        response.setContentLength((int)downloadable.length());
        response.setHeader("Content-disposition", "attachment; filename=" + downloadable.getName());


        try (InputStream input = new FileInputStream(resourcePath)) {
            StreamSupport.absorbInputStream(input, response.getOutputStream());
        }

    }

}
