package org.ijsberg.iglu.server.service;

import org.ijsberg.iglu.access.Authenticator;
import org.ijsberg.iglu.configuration.Cluster;
import org.ijsberg.iglu.configuration.Component;
import org.ijsberg.iglu.configuration.Startable;
import org.ijsberg.iglu.configuration.module.StandardComponent;
import org.ijsberg.iglu.logging.Logger;
import org.ijsberg.iglu.mvc.servlet.DispatcherServlet;
import org.ijsberg.iglu.rest.IgluRestServlet;
import org.ijsberg.iglu.server.http.ThreeTierAssembly;
import org.ijsberg.iglu.server.http.filter.WebAppEntryPoint;
import org.ijsberg.iglu.server.http.module.SimpleJettyServletContext;
import org.ijsberg.iglu.server.http.servlet.PropertiesServlet;
import org.ijsberg.iglu.util.properties.IgluProperties;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import java.util.Properties;

public class MaintenanceAssembly extends ThreeTierAssembly {

    private Startable coreAssembly;
    private String port = "17691";

    private MaintenanceServiceImpl maintenanceService;

    public MaintenanceAssembly(Properties properties, Component ssoAccessManager) {
        super(properties, ssoAccessManager);
    }

    public void setCoreAssembly(Startable coreAssembly) {
        this.coreAssembly = coreAssembly;
        maintenanceService.setCoreAssembly(coreAssembly);
    }

    @Override
    protected Cluster createDataLayer() {
        Cluster dataLayer = createCluster("DataLayer");//connected to core
        return dataLayer;
    }

    @Override
    protected Cluster createServiceLayer() {
        Cluster serviceLayer = createCluster("ServiceLayer");
        maintenanceService = new MaintenanceServiceImpl();
        Component service = new StandardComponent(maintenanceService);
        service.setProperties(properties);
        serviceLayer.connect("MaintenanceService", service, MaintenanceService.class);
        return serviceLayer;
    }

    @Override
    protected Cluster createPresentationLayer() {
        serviceLayer.connect(MaintenanceAgentImpl.AGENT_NAME, new StandardComponent(MaintenanceAgentImpl.getAgentFactory(serviceLayer, properties)));

        Cluster presentationLayer = createCluster("PresentationLayer");
        SimpleJettyServletContext servletContext = new SimpleJettyServletContext();
        Component jettyComponent = new StandardComponent(servletContext);
        Properties servletProperties = IgluProperties.loadProperties(home + "/conf/servlet_context.properties");
        port = servletProperties.getProperty("port", port);
        jettyComponent.setProperties(servletProperties);

        presentationLayer.connect("JettyServletContext", jettyComponent);

        //provide servlets access to other components
        for(Servlet servlet : servletContext.getServlets()) {
            if(servlet instanceof IgluRestServlet) {
                ((IgluRestServlet)servlet).setAssembly(this);
                ((IgluRestServlet)servlet).setAgentType(MaintenanceAgentImpl.AGENT_NAME, MaintenanceAgentImpl.class);
            }
        }
        for(Filter filter : servletContext.getFilters()) {
            if(filter instanceof WebAppEntryPoint) {
                Component entryPoint = new StandardComponent(filter);
                presentationLayer.connect("WebAppEntryPoint", entryPoint);
            }
        }
        return presentationLayer;
    }
}
