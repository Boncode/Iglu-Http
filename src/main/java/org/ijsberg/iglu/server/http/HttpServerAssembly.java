package org.ijsberg.iglu.server.http;

import org.ijsberg.iglu.access.AccessManager;
import org.ijsberg.iglu.access.component.RequestRegistry;
import org.ijsberg.iglu.access.component.StandardAccessManager;
import org.ijsberg.iglu.configuration.Cluster;
import org.ijsberg.iglu.configuration.Component;
import org.ijsberg.iglu.configuration.module.BasicAssembly;
import org.ijsberg.iglu.configuration.module.StandardCluster;
import org.ijsberg.iglu.configuration.module.StandardComponent;
import org.ijsberg.iglu.logging.Logger;
import org.ijsberg.iglu.logging.module.RotatingFileLogger;
import org.ijsberg.iglu.logging.module.StandardOutLogger;
import org.ijsberg.iglu.mvc.RequestMapper;
import org.ijsberg.iglu.mvc.component.StandardRequestMapper;
import org.ijsberg.iglu.mvc.servlet.DispatcherServlet;
import org.ijsberg.iglu.scheduling.module.StandardScheduler;
import org.ijsberg.iglu.server.admin.http.AdminAjaxResponseAgent;
import org.ijsberg.iglu.server.admin.module.AdminAgentImpl;
import org.ijsberg.iglu.server.facilities.module.UploadAgentImpl;
import org.ijsberg.iglu.server.http.filter.WebAppEntryPoint;
import org.ijsberg.iglu.server.http.module.SimpleJettyServletContext;
import org.ijsberg.iglu.usermanagement.UserManager;
import org.ijsberg.iglu.usermanagement.module.StandardUserManager;
import org.ijsberg.iglu.util.properties.PropertiesSupport;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by jeroe on 06/01/2018.
 */
public abstract class HttpServerAssembly extends BasicAssembly{
    private Map<String, Cluster> clusters = new HashMap<String, Cluster>();
    protected Cluster core;
    protected Cluster admin;

    protected StandardAccessManager accessManager;
    private RotatingFileLogger logger;

    //	private static CMSService service;
    private UserManager adminUserManager;

    public void initialize(String[] args) {

        core = new StandardCluster();

        createInfraLayer();
        createDataLayer();
        createServiceLayer();
        createPresentationLayer();
        createAdminLayer();

        core.connect("ServiceCluster", new StandardComponent(core));


        clusters.put("core", core);
    }

    @Override
    public Map<String, Cluster> getClusters() {
        return clusters;
    }

    @Override
    public Cluster getCoreCluster() {
        return core;
    }


    protected abstract void createDataLayer();

    protected abstract void createServiceLayer();

    protected abstract void createPresentationLayer();



    protected void createInfraLayer() {
        Component schedulerComponent = new StandardComponent(new StandardScheduler());
        core.connect("Scheduler", schedulerComponent);

        accessManager = new StandardAccessManager();
        Properties accessManProps = new Properties();
        accessManProps.setProperty("session_timeout", "" + (60 * 60 * 24));
        accessManager.setProperties(accessManProps);

        Component requestManagerComponent = new StandardComponent(accessManager);
        core.connect("AccessManager", requestManagerComponent, RequestRegistry.class);
        core.connect("RequestRegistry", requestManagerComponent, RequestRegistry.class);


        logger = new RotatingFileLogger("logs/website");

        Component loggerComponent = new StandardComponent(logger);
        core.connect("Logger", loggerComponent);
        loggerComponent.setProperties(PropertiesSupport.loadProperties("conf/logger.properties"));
    }

    private void createAdminLayer() {

        //TODO user must replace standard password

        admin = new StandardCluster();

        admin.connect("Logger", new StandardComponent(logger), Logger.class);
        admin.connect("UploadFactory", new StandardComponent(UploadAgentImpl.getAgentFactory(PropertiesSupport.loadProperties("admin/config/web_utility_agent.properties"))));

        AccessManager adminAccessManager = new StandardAccessManager();

        Component requestManagerComponent = new StandardComponent(adminAccessManager);
        admin.connect("AdminAccessManager", requestManagerComponent, RequestRegistry.class);
        //make it start & stop
        core.connect("AdminAccessManager", requestManagerComponent);
        admin.connect("RequestRegistry", requestManagerComponent, RequestRegistry.class);

        adminUserManager = new StandardUserManager();
        Component adminUserManagerComponent = new StandardComponent(adminUserManager);
        core.connect("Authenticator", adminUserManagerComponent);
        //TODO properties
        admin.connect("UserManager", adminUserManagerComponent);

        admin.connect("AdminAgentFactory", new StandardComponent(AdminAgentImpl.getAgentFactory()));
        admin.connect("AdminAgentResponseFactory", new StandardComponent(AdminAjaxResponseAgent.getAgentFactory()));

        RequestMapper requestMapper = new StandardRequestMapper();
        Component requestMapperComponent = new StandardComponent(requestMapper);
        requestMapperComponent.setProperties(PropertiesSupport.loadProperties("admin/config/request_mapper.properties"));
        admin.connect("AdminRequestMapper", requestMapperComponent);
        //register as external component
        core.getFacade().connect(requestMapperComponent);

        SimpleJettyServletContext servletContext = new SimpleJettyServletContext();
        Component jettyComponent = new StandardComponent(servletContext);
        Properties servletProperties = PropertiesSupport.loadProperties("admin/config/servlet_context.properties");
        jettyComponent.setProperties(servletProperties);
        admin.connect("AdminServletContext", jettyComponent);
        //register as external component
        core.getFacade().connect(jettyComponent);

        //provide servlets access to other components
        //cuurently actual references are set instead of proxies
        for(Servlet servlet : servletContext.getServlets()) {
            if(servlet instanceof DispatcherServlet) {
                Component requestDispatcher = new StandardComponent(servlet);
                admin.connect("AdminRequestDispatcher", requestDispatcher);
                //override request mapper
                ((DispatcherServlet)servlet).setRequestMapper(requestMapper);
            }
        }


        for(Filter filter : servletContext.getFilters()) {
            if(filter instanceof WebAppEntryPoint) {
                //by having the entrypoint as a component, we are able to browse it
                //it's the holy grail of CBD
                Component entryPoint = new StandardComponent(filter);
                admin.connect("AdminWebAppEntryPoint", entryPoint);
                //override access manager
                ((WebAppEntryPoint)filter).setAccessManager(adminAccessManager);
            }
        }

        core.connect("admin", new StandardComponent(admin));
        //register the assembly itself
        admin.connect("Assembly", new StandardComponent(this));
        admin.connect("ServiceCluster", new StandardComponent(admin), Cluster.class);
        admin.connect("CoreCluster", new StandardComponent(core));
    }

    public void addAppender(Logger logger) {
        this.logger.addAppender(logger);
    }
}
