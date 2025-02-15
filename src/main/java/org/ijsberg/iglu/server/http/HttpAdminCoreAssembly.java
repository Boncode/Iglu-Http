package org.ijsberg.iglu.server.http;

import org.ijsberg.iglu.assembly.StandardCoreAssembly;
import org.ijsberg.iglu.configuration.Cluster;

import java.util.Properties;

public class HttpAdminCoreAssembly extends StandardCoreAssembly {

    protected Cluster admin;

    public HttpAdminCoreAssembly(Properties properties) {
        super(properties);
        createAdminLayer();
    }

    private void createAdminLayer() {

        //TODO user must replace standard password
        //JM: possible backdoor disabled
/*        admin = createCluster("admin");

        admin.connect("Logger", new StandardComponent(logger), Logger.class);
        admin.connect("UploadFactory", new StandardComponent(FileManagerAgentImpl.getAgentFactory(admin, IgluProperties.loadProperties("admin/config/upload_agent.properties"))));

        AccessManager adminAccessManager = new StandardAccessManager();

        Component requestManagerComponent = new StandardComponent(adminAccessManager);
        admin.connect("AdminAccessManager", requestManagerComponent, RequestRegistry.class);
        //make it start & stop
        core.connect("AdminAccessManager", requestManagerComponent);
        admin.connect("RequestRegistry", requestManagerComponent, RequestRegistry.class);

        UserManager adminUserManager = new StandardUserManager("sjodifo9475kdfnHGrp".getBytes());
        Component adminUserManagerComponent = new StandardComponent(adminUserManager);
        core.connect("Authenticator", adminUserManagerComponent);
        //TODO properties
        admin.connect("UserManager", adminUserManagerComponent);

        admin.connect("AdminAgentFactory", new StandardComponent(AdminAgentImpl.getAgentFactory(admin)));
        admin.connect("AdminAgentResponseFactory", new StandardComponent(AdminAjaxResponseAgent.getAgentFactory(admin)));

        SimpleJettyServletContext servletContext = new SimpleJettyServletContext();
        Component jettyComponent = new StandardComponent(servletContext);
        Properties servletProperties = IgluProperties.loadProperties("admin/config/servlet_context.properties");
        jettyComponent.setProperties(servletProperties);
        admin.connect("AdminServletContext", jettyComponent);
        //register as external component
        core.getFacade().connect(jettyComponent);

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

        //core.connect("admin", new StandardComponent(admin));
        //register the assembly itself
        admin.connect("Assembly", new StandardComponent(this));
        admin.connect("ServiceCluster", new StandardComponent(admin), Cluster.class);
        admin.connect("CoreCluster", new StandardComponent(core));
*/
    }

}
