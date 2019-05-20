package org.ijsberg.iglu.server.http;

import org.ijsberg.iglu.access.AccessManager;
import org.ijsberg.iglu.access.component.RequestRegistry;
import org.ijsberg.iglu.access.component.StandardAccessManager;
import org.ijsberg.iglu.configuration.Cluster;
import org.ijsberg.iglu.configuration.Component;
import org.ijsberg.iglu.configuration.module.BasicAssembly;
import org.ijsberg.iglu.configuration.module.StandardComponent;
import org.ijsberg.iglu.scheduling.module.StandardScheduler;
import org.ijsberg.iglu.util.collection.ArraySupport;

import java.util.Properties;

/**
 * Created by jeroe on 06/01/2018.
 */
public abstract class ThreeTierAssembly extends BasicAssembly {

    protected Component accessManager;

    protected Cluster infraLayer;
    protected Cluster dataLayer;
    protected Cluster serviceLayer;
    protected Cluster presentationLayer;



    public ThreeTierAssembly(Properties properties) {

        super(properties);
        createLayers(properties);
    }

    public void createLayers(Properties properties) {
        infraLayer = createInfraLayer();

        dataLayer = createDataLayer();
        dataLayer.connect("InfraCluster", infraLayer);

        serviceLayer = createServiceLayer();
        serviceLayer.connect("InfraCluster", infraLayer);
        serviceLayer.connect("DataCluster", dataLayer);

        presentationLayer = createPresentationLayer();
        presentationLayer.connect("InfraCluster", infraLayer);
        presentationLayer.connect("ServiceCluster", serviceLayer);
    }

    public ThreeTierAssembly(Properties properties, Component ssoAccessManager) {
        super(properties);
        accessManager = ssoAccessManager;
        createLayers(properties);
    }

    protected abstract Cluster createDataLayer();

    protected abstract Cluster createServiceLayer();

    protected abstract Cluster createPresentationLayer();



    protected Cluster createInfraLayer() {

        //super.createInfraLayer();
        Component schedulerComponent = new StandardComponent(new StandardScheduler());
        core.connect("Scheduler", schedulerComponent);

        if(accessManager == null) {
            StandardAccessManager standardAccessManager = new StandardAccessManager();
            Properties accessManProps = new Properties();
            accessManProps.setProperty("session_timeout", "" + (60 * 60 * 24));
            standardAccessManager.setProperties(accessManProps);
            accessManager = new StandardComponent(standardAccessManager);
        }

        //Component requestManagerComponent = new StandardComponent(accessManager);
        //System.out.println("==============> " + ArraySupport.format(requestManagerComponent.getInterfaces(),","));
        core.connect("AccessManager", accessManager, RequestRegistry.class, AccessManager.class);
        core.connect("RequestRegistry", accessManager, RequestRegistry.class);

        return core;
    }

}
