package org.ijsberg.iglu.server.http.servlet;

import org.ijsberg.iglu.util.collection.CollectionSupport;
import org.ijsberg.iglu.util.io.*;
import org.ijsberg.iglu.util.misc.StringSupport;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RandomAccessZipFileResourceServlet extends BinaryResourceServlet {

    private String resourceDir;

    protected Map<String, String> resources = new HashMap<>();

    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);
        resourceDir = conf.getInitParameter("resource_dir");
        if(resourceDir == null) {
            throw new ServletException("please provide paramater resource_dir");
        }
        FileCollection fileCollection = new FSFileCollection(resourceDir, new FileFilterRuleSet().setIncludeFilesWithNameMask("*.zip|*.jar"));
        for(String fileName : fileCollection.getFileNames()) {
            FileData fileData = new FileData(fileName);
            resources.put(fileData.getFileNameWithoutExtension(), fileName);
        }
    }


    @Override
    public byte[] getResource(String path) throws IOException, ServletException {

        if("".equals(path)) {
            return new byte[0];
        }
        List<String> pathElements = StringSupport.split(path, "/");
        String resourceName = pathElements.remove(0);
        return FileSupport.getBinaryFromJar(CollectionSupport.format(pathElements, "/"), resourceDir + "/" + resources.get(resourceName));
    }
}
