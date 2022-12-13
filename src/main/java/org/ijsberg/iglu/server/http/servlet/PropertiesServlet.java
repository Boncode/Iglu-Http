package org.ijsberg.iglu.server.http.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.http.json.JsonData;
import org.ijsberg.iglu.http.json.JsonHierarchicalPropertiesObject;
import org.ijsberg.iglu.util.properties.IgluProperties;

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
        IgluProperties properties = IgluProperties.loadProperties(propertiesFile);
        merge(properties);
        jsonProperties = new JsonHierarchicalPropertiesObject(properties, false);
        //enrich();

    }

    private void merge(IgluProperties properties) {
        if(additionalProperties != null) {
            properties.merge(additionalProperties);
        }
    }

    public void enrich(Properties additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

/*    private void enrich() {
        if(additionalProperties != null) {
            for (String key : additionalProperties.stringPropertyNames()) {
                if (!jsonProperties.containsAttribute(key)) {
                    jsonProperties.addStringAttribute(key, additionalProperties.getProperty(key));
                    jsonProperties.s
                }
            }
        }
    }
*/
    public void service(HttpServletRequest servletRequest, HttpServletResponse response) throws IOException, ServletException {
        ServletOutputStream out = response.getOutputStream();
        response.setContentType("application/json");
        out.println(jsonProperties.toString());
    }
}
