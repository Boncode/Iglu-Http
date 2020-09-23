package org.ijsberg.iglu.server.http.servlet;

import org.ijsberg.iglu.http.json.JsonData;
import org.ijsberg.iglu.http.json.JsonHierarchicalPropertiesObject;
import org.ijsberg.iglu.util.properties.IgluProperties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Properties;

public class PropertiesServlet extends HttpServlet {

    private JsonData jsonProperties;
    private Properties additionalProperties;

    /**
     * @param conf
     * @throws ServletException
     */
    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);
        String propertiesFile = getInitParameter("properties_file");
        Properties properties = IgluProperties.loadProperties(propertiesFile);
        jsonProperties = new JsonHierarchicalPropertiesObject(properties, false);
        enrich();
    }

    public void enrich(Properties additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    private void enrich() {
        if(additionalProperties != null) {
            for (String key : additionalProperties.stringPropertyNames()) {
                if (!jsonProperties.containsAttribute(key)) {
                    jsonProperties.addStringAttribute(key, additionalProperties.getProperty(key));
                }
            }
        }
    }

    public void service(HttpServletRequest servletRequest, HttpServletResponse response) throws IOException, ServletException {
        ServletOutputStream out = response.getOutputStream();
        response.setContentType("application/json");
        out.println(jsonProperties.toString());
    }
}
