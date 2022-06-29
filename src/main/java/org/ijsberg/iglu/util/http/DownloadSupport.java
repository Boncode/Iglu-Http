package org.ijsberg.iglu.util.http;

import org.ijsberg.iglu.util.ResourceException;
import org.ijsberg.iglu.util.io.FileSupport;
import org.ijsberg.iglu.util.mail.MimeTypeSupport;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

public class DownloadSupport {

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

    public static void setResponseDownloadMetaData(HttpServletResponse response, File downloadable) {
        String fileName = downloadable.getName();
        response.setContentType(MimeTypeSupport.getMimeTypeForFileExtension(fileName.substring(fileName.lastIndexOf('.') + 1)));
        response.setContentLength((int) downloadable.length());
        response.setHeader("Content-disposition", "attachment; filename=" + fileName);
    }

}
