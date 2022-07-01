package org.ijsberg.iglu.util.http;

import org.ijsberg.iglu.util.ResourceException;
import org.ijsberg.iglu.util.io.FileSupport;

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

}
