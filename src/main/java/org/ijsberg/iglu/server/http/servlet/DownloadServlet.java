package org.ijsberg.iglu.server.http.servlet;

import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.http.HttpEncodingSupport;
import org.ijsberg.iglu.util.io.FileSupport;
import org.ijsberg.iglu.util.io.StreamSupport;
import org.ijsberg.iglu.util.mail.MimeTypeSupport;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.InputStream;

public class DownloadServlet extends HttpServlet {

    protected String resourceDir;

    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);

        resourceDir = conf.getInitParameter("resource_dir");
        if(resourceDir == null) {
            resourceDir = "";
        }
    }

    public void service(HttpServletRequest request, HttpServletResponse response) {

        String resourcePath = null;
        String pathInfo = request.getPathInfo();
        if(pathInfo == null) {
            pathInfo = "";
        }
        try {
            resourcePath = FileSupport.convertToUnixStylePath(resourceDir + '/' + pathInfo);
            if(resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            response.setContentType(MimeTypeSupport.getMimeTypeForFileExtension(resourcePath.substring(resourcePath.lastIndexOf('.') + 1)));

            resourcePath = HttpEncodingSupport.urlDecode(resourcePath);
            InputStream input = new FileInputStream(resourcePath);
            try {
                StreamSupport.absorbInputStream(input, response.getOutputStream());
            } finally {
                input.close();
            }
        } catch (Exception e) {
            System.out.println(new LogEntry(Level.CRITICAL, "unable to obtain resource " + resourcePath, e));
            response.setStatus(500);
        }
    }

/*	private void writeStats(HttpServletResponse response) throws IOException {
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		for(String requestedResource : requestedResources) {
			out.println(requestedResource);
		}
	}
*/


}
