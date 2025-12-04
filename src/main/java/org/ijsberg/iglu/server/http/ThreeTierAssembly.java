package org.ijsberg.iglu.server.http;

import org.ijsberg.iglu.access.AccessManager;
import org.ijsberg.iglu.access.RequestRegistry;
import org.ijsberg.iglu.access.component.StandardAccessManager;
import org.ijsberg.iglu.asset.AssetAccessManager;
import org.ijsberg.iglu.asset.component.StandardAssetAccessManager;
import org.ijsberg.iglu.configuration.Assembly;
import org.ijsberg.iglu.configuration.Cluster;
import org.ijsberg.iglu.configuration.Component;
import org.ijsberg.iglu.configuration.component.ApplicationSettingsManager;
import org.ijsberg.iglu.configuration.component.ApplicationSettingsProvider;
import org.ijsberg.iglu.configuration.component.StandardApplicationSettingsManager;
import org.ijsberg.iglu.configuration.module.BasicAssembly;
import org.ijsberg.iglu.configuration.module.StandardComponent;
import org.ijsberg.iglu.event.EventBus;
import org.ijsberg.iglu.event.ServiceBroker;
import org.ijsberg.iglu.event.model.BasicEvent;
import org.ijsberg.iglu.event.model.EventTopic;
import org.ijsberg.iglu.event.module.BasicServiceBroker;
import org.ijsberg.iglu.invocation.RootConsole;
import org.ijsberg.iglu.logging.module.RotatingFileLogger;
import org.ijsberg.iglu.logging.module.StandardOutLogger;
import org.ijsberg.iglu.scheduling.module.StandardScheduler;
import org.ijsberg.iglu.usermanagement.UserManager;
import org.ijsberg.iglu.usermanagement.module.StandardUserManager;
import org.ijsberg.iglu.usermanagement.multitenancy.component.MultiTenantAwareComponent;
import org.ijsberg.iglu.util.properties.IgluProperties;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public abstract class ThreeTierAssembly extends BasicAssembly {

    protected static final String LOGGER = "Logger";
    protected static final String SCHEDULER = "Scheduler";
    protected static final String ACCESS_MANAGER = "AccessManager";
    protected static final String REQUEST_REGISTRY = "RequestRegistry";
    public static final String SERVICE_BROKER = "ServiceBroker";
    public static final String EVENT_BUS = "EventBus";
    protected static final String ASSET_ACCESS_MANAGER = "AssetAccessManager";
    protected static final String ROOT_CONSOLE = "RootConsole";
    protected static final String APPLICATION_SETTINGS_MANAGER = "ApplicationSettingsManager";
    protected static final String USER_MANAGER = "UserManager";

    protected Component logger;
    protected Component rootConsole;

    protected Component scheduler;
    protected Component accessManager;
    protected Component serviceBroker;
    protected Component assetAccessManager;
    protected Component applicationSettingsManager;
    protected Component userManager;

    protected Cluster infraLayer;
    protected Cluster dataLayer;
    protected Cluster serviceLayer;
    protected Cluster presentationLayer;


    public ThreeTierAssembly(Properties properties) {
        super(properties);
        createLayers(properties);
    }

    public ThreeTierAssembly(Properties properties, Assembly coreAssembly) {
        super(properties);
        Map<String,Component> coreClusterComponents = coreAssembly.getCoreCluster().getInternalComponents();
        applicationSettingsManager = coreClusterComponents.get(APPLICATION_SETTINGS_MANAGER);
        logger = coreClusterComponents.get(LOGGER);
        rootConsole = coreClusterComponents.get(ROOT_CONSOLE);
        scheduler = coreClusterComponents.get(SCHEDULER);
        accessManager = coreClusterComponents.get(ACCESS_MANAGER);
        serviceBroker = coreClusterComponents.get(SERVICE_BROKER);
        assetAccessManager = coreClusterComponents.get(ASSET_ACCESS_MANAGER);
        userManager = coreClusterComponents.get(USER_MANAGER);
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

        if(applicationSettingsManager == null) {
            applicationSettingsManager = new StandardComponent(new StandardApplicationSettingsManager(home + "/" + configDir));
        }
        core.connect(APPLICATION_SETTINGS_MANAGER, applicationSettingsManager, ApplicationSettingsProvider.class, ApplicationSettingsManager.class);


        if(logger == null) {
            createLoggerComponent();
        }
        core.connect(LOGGER, logger);

        if(rootConsole == null) {
            rootConsole = new StandardComponent(new RootConsole(this));
            core.connect(ROOT_CONSOLE, rootConsole);
        }

        if(scheduler == null) {
            scheduler = new StandardComponent(new StandardScheduler());
        }
        core.connect(SCHEDULER, scheduler);

        if(accessManager == null) {
            createAccessManagerComponent();
        }

        if(userManager == null) {
            initUserManager();
        }
        core.connect(USER_MANAGER, userManager);

        core.connect(ACCESS_MANAGER, accessManager, RequestRegistry.class, AccessManager.class);
        core.connect(REQUEST_REGISTRY, accessManager, RequestRegistry.class);

        if(serviceBroker == null) {
            serviceBroker = new StandardComponent(new BasicServiceBroker());
            //todo move to some CoreAssembly...
            serviceBroker.getProxy(EventBus.class)
                    .registerEventTopic(
                            new EventTopic<>(
                                    "IGLU_EVENTS",
                                    "Iglu events",
                                    "Events related to the Iglu Framework",
                                    BasicEvent.class
                            )
                    );
        }
        core.connect(SERVICE_BROKER, serviceBroker, ServiceBroker.class);
        core.connect(EVENT_BUS, serviceBroker, EventBus.class);

        if(assetAccessManager == null) {
            assetAccessManager = new StandardComponent(new StandardAssetAccessManager());
            assetAccessManager.setProperties(properties);
        }
        core.connect(ASSET_ACCESS_MANAGER, assetAccessManager, AssetAccessManager.class);

        return core;
    }

    private UserManager initUserManager() {
        StandardUserManager stdUserManager = new StandardUserManager("dakp[F(io3udsua.&*990adduX=".getBytes());
        Properties properties = new Properties();
        properties.setProperty("data_dir", home + "/data");
//        try {
//            FileSupport.copyFile(home + "/data/JsonSimpleAccount.json", "./data/JsonSimpleAccount.json", true);
//        } catch (IOException e) {
//            System.out.println(new LogEntry(Level.CRITICAL, "cannot copy user file to directory that is being backed up"));
//        }
        stdUserManager.setProperties(properties);

        userManager = new StandardComponent(stdUserManager);
        //layer.connect("UserManager", userManagerComponent, UserManager.class);
        stdUserManager.start();
        return stdUserManager;
//        userManager.start();
    }

    private void createAccessManagerComponent() {
        StandardAccessManager standardAccessManager = new StandardAccessManager(MultiTenantAwareComponent.class);
        standardAccessManager.setProperties(getAccessManagerSettings());
        accessManager = new StandardComponent(standardAccessManager);
    }

    private Properties getAccessManagerSettings() {
        ApplicationSettingsProvider settingsProvider = applicationSettingsManager.getProxy(ApplicationSettingsProvider.class);
        Properties accessManProps = new Properties();
        accessManProps.setProperty("session_timeout", String.valueOf(settingsProvider.getApplicationSettings().getSessionTimeout()));
        accessManProps.setProperty("session_timeout_logged_in", String.valueOf(settingsProvider.getApplicationSettings().getSessionTimeoutLoggedIn()));
        return accessManProps;
    }

    private void createLoggerComponent() {
        RotatingFileLogger rotatingFileLogger = new RotatingFileLogger("logs/" + this.getClass().getSimpleName());
        logger = new StandardComponent(rotatingFileLogger);
        Properties loggerProperties;
        if (IgluProperties.propertiesExist(home + "/" + configDir + "/logger.properties")) {
            loggerProperties = IgluProperties.loadProperties(home + "/" + configDir + "/logger.properties");
        } else {
            loggerProperties = new IgluProperties();
            loggerProperties.setProperty("log_level", "DEBUG");
            loggerProperties.setProperty("log_to_standard_out", "true");
            loggerProperties.setProperty("nr_log_files_to_keep", "365");
            try {
                IgluProperties.saveProperties(loggerProperties, home + "/" + configDir + "/logger.properties");
            } catch (IOException e) {
                System.err.println("could not save logger.properties with message: " + e.getMessage());
            }
        }
        logger.setProperties(loggerProperties);

        if (Boolean.parseBoolean(loggerProperties.getProperty("log_to_standard_out", "false"))) {
            rotatingFileLogger.addAppender(new StandardOutLogger());
        }
    }
}
