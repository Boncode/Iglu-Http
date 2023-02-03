package org.ijsberg.iglu.server.http.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.collection.CollectionSupport;
import org.ijsberg.iglu.util.io.*;
import org.ijsberg.iglu.util.misc.StringSupport;

import java.io.IOException;
import java.util.List;
import java.util.TreeMap;


public class RandomAccessZipFileResourceServlet extends BinaryResourceServlet {

    protected String resourceDir;
    //TODO get from config
    private String includeMask = "*.zip";
    private FSFileCollection fileCollection;

    protected TreeMap<String, String> resources = new TreeMap<>();


    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);
        resourceDir = conf.getInitParameter("resource_dir");
        if(resourceDir == null) {
            throw new ServletException("please provide parameter resource_dir");
        }
        fileCollection = new FSFileCollection(resourceDir, new FileFilterRuleSet().setIncludeFilesWithNameMask("*.zip|*.jar"));
        mapResources(fileCollection);
    }

    protected void refresh() {
        fileCollection.refreshFiles();
        mapResources(fileCollection);
    }

    public void mapResources(FileCollection fileCollection) {
        for(String fileName : fileCollection.getFileNames()) {
            FileData fileData = new FileData(fileName);
            resources.put(fileData.getFileNameWithoutExtension(), fileName);
        }
    }


    @Override
    public byte[] getResource(String path) throws IOException {

        if("".equals(path)) {
            return new byte[0];
        }
        List<String> pathElements = StringSupport.split(path, "/");
        String resourceName = pathElements.remove(0);
        String resourceZipFileName = resources.get(resourceName);
        if(resourceZipFileName == null) {
            System.out.println(new LogEntry(Level.CRITICAL, "resource " + resourceName + " not found, refreshing files"));
            refresh();
        }
        return FileSupport.getBinaryFromJar(CollectionSupport.format(pathElements, "/"), resourceDir + "/" + resources.get(resourceName));
    }
}
