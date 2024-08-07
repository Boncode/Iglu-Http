package org.ijsberg.iglu.util.http;

import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.ResourceException;
import org.ijsberg.iglu.util.io.FileSupport;
import org.ijsberg.iglu.util.io.StreamSupport;
import org.ijsberg.iglu.util.mail.MimeTypeSupport;

import java.io.*;

public class DownloadSupport {

    public static File getDownloadableFile(String resourcePath) throws FileNotFoundException {
        resourcePath = FileSupport.convertToUnixStylePath(resourcePath);
        if(resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }
        resourcePath = HttpEncodingSupport.urlDecode(resourcePath);
        if(resourcePath.contains("..")) {
            throw new SecurityException("resource path contains suspicious content");
        }

        File downloadable = new File(resourcePath);
        if (!downloadable.exists()) {
            throw new FileNotFoundException("file does not exist");
        }

        return downloadable;
    }

    public static void downloadFile(HttpServletResponse response, String resourcePath) throws IOException {
        File downloadable = getDownloadableFile(resourcePath);
        String fileName = downloadable.getName();
        System.out.println(new LogEntry(Level.DEBUG, String.format("downloading %s", fileName)));

        response.setContentType(MimeTypeSupport.getMimeTypeForFileName(fileName));
        response.setContentLength((int) downloadable.length());
        response.setHeader("Content-disposition", "attachment; filename=" + fileName);

        try (InputStream input = new FileInputStream(downloadable)) {
            StreamSupport.absorbInputStream(input, response.getOutputStream());
        }
    }

    public static void downloadData(HttpServletResponse response, String fileName, byte[] data) throws IOException {
        System.out.println(new LogEntry(Level.DEBUG, String.format("downloading %s", fileName)));

        response.setContentType(MimeTypeSupport.getMimeTypeForFileName(fileName));
        response.setContentLength(data.length);
        response.setHeader("Content-disposition", "attachment; filename=" + fileName);

        StreamSupport.writeToOutputStream(data, response.getOutputStream());
    }
}
