package org.ijsberg.iglu.server.http;

import org.ijsberg.iglu.access.AccessManager;
import org.ijsberg.iglu.access.component.RequestRegistry;
import org.ijsberg.iglu.access.component.StandardAccessManager;
import org.ijsberg.iglu.configuration.Cluster;
import org.ijsberg.iglu.configuration.Component;
import org.ijsberg.iglu.configuration.module.BasicAssembly;
import org.ijsberg.iglu.configuration.module.StandardComponent;
import org.ijsberg.iglu.scheduling.module.StandardScheduler;
import org.ijsberg.iglu.usermanagement.multitenancy.component.MultiTenantAwareComponent;
import org.ijsberg.iglu.util.collection.ArraySupport;

import java.util.Properties;

/**
 * Created by jeroe on 06/01/2018.
 */
public abstract class ThreeTierAssembly extends BasicAssembly {

    protected Component accessManager;
    protected Component scheduler;

    protected Cluster infraLayer;
    protected Cluster dataLayer;
    protected Cluster serviceLayer;
    protected Cluster presentationLayer;



    public ThreeTierAssembly(Properties properties) {
        super(properties);
        createLayers(properties);
    }

    public ThreeTierAssembly(Properties properties, Component ssoAccessManager) {
        super(properties);
        accessManager = ssoAccessManager;
        createLayers(properties);
    }

    public ThreeTierAssembly(Properties properties, Component ssoAccessManager, Component scheduler) {
        super(properties);
        accessManager = ssoAccessManager;
        this.scheduler = scheduler;
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

    protected abstract Cluster createDataLayer();

    protected abstract Cluster createServiceLayer();

    protected abstract Cluster createPresentationLayer();



    protected Cluster createInfraLayer() {

        //super.createInfraLayer();
        if(scheduler == null) {
            scheduler = new StandardComponent(new StandardScheduler());
        }
        core.connect("Scheduler", scheduler);

        if(accessManager == null) {
            StandardAccessManager standardAccessManager = new StandardAccessManager(MultiTenantAwareComponent.class);
            Properties accessManProps = new Properties();
            accessManProps.setProperty("session_timeout", "" + Integer.parseInt(properties.getProperty("sessionTimeout", "60")));
            accessManProps.setProperty("session_timeout_logged_in", "" + Integer.parseInt(properties.getProperty("sessionTimeoutLoggedIn", "1800")));
//            accessManProps.setProperty("session_timeout", "" + (60 * 60 * 24));
            standardAccessManager.setProperties(accessManProps);
            accessManager = new StandardComponent(standardAccessManager);
        }

        //Component requestManagerComponent = new StandardComponent(accessManager);
        core.connect("AccessManager", accessManager, RequestRegistry.class, AccessManager.class);
        core.connect("RequestRegistry", accessManager, RequestRegistry.class);

        return core;
    }

}
