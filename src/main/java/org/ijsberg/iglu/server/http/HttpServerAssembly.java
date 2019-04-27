package org.ijsberg.iglu.server.http;

import org.ijsberg.iglu.access.AccessManager;
import org.ijsberg.iglu.access.component.RequestRegistry;
import org.ijsberg.iglu.access.component.StandardAccessManager;
import org.ijsberg.iglu.configuration.Cluster;
import org.ijsberg.iglu.configuration.Component;
import org.ijsberg.iglu.configuration.module.BasicAssembly;
import org.ijsberg.iglu.configuration.module.StandardComponent;
import org.ijsberg.iglu.scheduling.module.StandardScheduler;

import java.util.Properties;

/**
 * Created by jeroe on 06/01/2018.
 */
public abstract class HttpServerAssembly extends BasicAssembly {

    protected StandardAccessManager accessManager;

    protected Cluster infraLayer;
    protected Cluster dataLayer;
    protected Cluster serviceLayer;
    protected Cluster presentationLayer;

    protected Properties properties;

    public HttpServerAssembly(Properties properties) {

        super();

        this.properties = properties;

        infraLayer = createInfraLayer();

        dataLayer = createDataLayer();
        dataLayer.connect("InfraCluster", infraLayer);

        serviceLayer = createServiceLayer();
        serviceLayer.connect("InfraCluster", infraLayer);
        serviceLayer.connect("DataCluster", dataLayer);

        presentationLayer = createPresentationLayer();
        presentationLayer.connect("InfraCluster", infraLayer);
        presentationLayer.connect("ServiceCluster", serviceLayer);

//        core.connect("ServiceCluster", serviceLayer);

    }


    protected abstract Cluster createDataLayer();

    protected abstract Cluster createServiceLayer();

    protected abstract Cluster createPresentationLayer();



    protected Cluster createInfraLayer() {

        //super.createInfraLayer();
        Component schedulerComponent = new StandardComponent(new StandardScheduler());
        core.connect("Scheduler", schedulerComponent);

        accessManager = new StandardAccessManager();
        Properties accessManProps = new Properties();
        accessManProps.setProperty("session_timeout", "" + (60 * 60 * 24));
        accessManager.setProperties(accessManProps);

        Component requestManagerComponent = new StandardComponent(accessManager);
        core.connect("AccessManager", requestManagerComponent, RequestRegistry.class, AccessManager.class);
        core.connect("RequestRegistry", requestManagerComponent, RequestRegistry.class);

        return core;
    }

}
