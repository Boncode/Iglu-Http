package org.ijsberg.iglu.server.http;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.ijsberg.iglu.configuration.Assembly;
import org.ijsberg.iglu.configuration.Cluster;
import org.ijsberg.iglu.configuration.Component;
import org.ijsberg.iglu.configuration.ConfigurationException;
import org.ijsberg.iglu.configuration.module.BasicAssembly;
import org.ijsberg.iglu.configuration.module.ServerEnvironment;
import org.ijsberg.iglu.configuration.module.StandardCluster;
import org.ijsberg.iglu.configuration.module.StandardComponent;
import org.ijsberg.iglu.logging.Logger;
import org.ijsberg.iglu.logging.module.StandardOutLogger;
import org.ijsberg.iglu.server.http.module.SimpleJettyServletContext;

import java.awt.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by J Meetsma on 26-4-2017.
 */
public class SimpleWebserverAssembly extends BasicAssembly {
    private Map<String, Cluster> clusters = new HashMap<String, Cluster>();
    private Cluster core;
    private static Logger logger;

    private static String resourceFilter = "*";


    private static String projectDir;

    private int port = 17680;

    public SimpleWebserverAssembly() {
    }

    public SimpleWebserverAssembly(int port) {
        this.port = port;
    }

    public void initialize(String[] args) {
        core = new StandardCluster();
        createPresentationLayer();
        core.connect("ServiceCluster", new StandardComponent(core));
        clusters.put("core", core);
    }


    private void createPresentationLayer() {

        SimpleJettyServletContext servletContext = new SimpleJettyServletContext();
        Component jettyComponent = new StandardComponent(servletContext);

        Properties webserverProperties = new Properties();
        webserverProperties.setProperty("port", "" + port);
        webserverProperties.setProperty("servlet.staticcontentservlet.class", DefaultServlet.class.getName());
        webserverProperties.setProperty("servlet.staticcontentservlet.url_pattern", "/");
        webserverProperties.setProperty("document_root", projectDir);

        jettyComponent.setProperties(webserverProperties);
        core.connect("JettyServletContext", jettyComponent);
    }

    @Override
    public Map<String, Cluster> getClusters() {
        return clusters;
    }

    @Override
    public Cluster getCoreCluster() {
        return core;
    }

    /**
     *
     * @param args projectDir, tcp port, project mask
     */
    public static void main(String ... args) throws Exception {

        int port = 17680;

        if(args.length == 0) {
            throw new ConfigurationException("please provide a project directory as first argument");
        }

        projectDir = args[0];

        if(args.length > 1) {
            port = Integer.parseInt(args[1]);
        }

        if(args.length > 2) {
            resourceFilter = args[2];
        }

        ServerEnvironment env = null;
        try {
            Assembly assembly = new SimpleWebserverAssembly(port);
            assembly.initialize(args);
            env = new ServerEnvironment(assembly);
            //IDE started app must log to std out
            logger = new StandardOutLogger();
            env.start();
            Desktop.getDesktop().browse(new URI("http://localhost:" + port));
            System.out.println("Press ENTER to stop server...");
            System.in.read();

        } finally {
            if(env != null) {
                env.shutDown();
            }
        }
    }
}
