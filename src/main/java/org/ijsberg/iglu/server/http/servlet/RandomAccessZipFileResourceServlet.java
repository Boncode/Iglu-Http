package org.ijsberg.iglu.server.http.servlet;

import org.ijsberg.iglu.util.collection.CollectionSupport;
import org.ijsberg.iglu.util.io.*;
import org.ijsberg.iglu.util.misc.StringSupport;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RandomAccessZipFileResourceServlet extends BinaryResourceServlet {


    private String resourceDir = "/repository";

    private Map<String, String> resources = new HashMap<>();

    public RandomAccessZipFileResourceServlet() {
        FileCollection fileCollection = new FSFileCollection(resourceDir, new FileFilterRuleSet().setIncludeFilesWithNameMask("*.zip|*.jar"));
        for(String fileName : fileCollection.getFileNames()) {
            FileData fileData = new FileData(fileName);
            resources.put(fileData.getFileName(), fileName);
        }
        System.out.println(resources);
    }

    @Override
    public byte[] getResource(String path) throws IOException, ServletException {

        if("".equals(path)) {
            return new byte[0];
        }

        List<String> pathElements = StringSupport.split(path, "/");

        System.out.println("==> " + path);
        System.out.println("==> " + CollectionSupport.format(pathElements, ", "));

        String resourceName = pathElements.remove(0);


        return FileSupport.getBinaryFromJar(CollectionSupport.format(pathElements, "/"), resourceDir + "/" + resources.get(resourceName));
   /*
        return new byte[0];*/
    }
}
