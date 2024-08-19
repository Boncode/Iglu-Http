package org.ijsberg.iglu.rest.model;

//import nl.ijsberg.analysis.browsing.rest.AnalysisBrowsingAgentImpl;
//import nl.ijsberg.analysis.browsing.rest.AnalysisDownloadAgentImpl;
//import nl.ijsberg.analysis.license.module.LicenseServiceImpl;
//import nl.ijsberg.analysis.metrics.rest.AnalysisMetricDataApiImpl;
//import nl.ijsberg.analysis.sbom_feedback.module.SbomFeedbackAgentImpl;
//import nl.ijsberg.analysis.server.analysisserver.module.AnalysisAgentImpl;
//import nl.ijsberg.dashboardapi.auth.module.AuthenticationAgentImpl;
//import nl.ijsberg.dashboardapi.service.AssetAccessServiceImpl;
//import nl.ijsberg.dashboardapi.service.DashboardAgentImpl;
//import nl.ijsberg.dashboardapi.service.DashboardUserAgentImpl;
//import nl.ijsberg.datahub.metrics.api.MetricDataApiImpl;
//import nl.ijsberg.mtp.module.SecurityEventAgentImpl;
//import nl.ijsberg.mtp.module.TaskPlanningAgentImpl;
//import nl.ijsberg.security.sbom.module.SbomAgentImpl;
//import nl.ijsberg.security.vulnerabilities.module.VulnerabilityAgentImpl;
//import nl.ijsberg.security.vulnerabilities.module.VulnerabilityServiceImpl;
//import nl.ijsberg.security.weaknesses.module.WeaknessAgentImpl;
//import nl.ijsberg.service.MaintenanceAgentImpl;
//import nl.ijsberg.service.monitor.module.MonitorServiceImpl;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.rest.Endpoint;
import org.ijsberg.iglu.server.facilities.module.FileManagerAgentImpl;

import java.lang.reflect.Method;
import java.util.*;

//import static org.ijsberg.iglu.rest.model.WebServiceData.WebserviceDomain.INTERNAL;
//import static org.ijsberg.iglu.rest.model.WebServiceData.WebserviceDomain.MAINTENANCE_TASK_PLANNING;

/**
 * Note: run CollectAuthMatrix.collect() to make sure all WebServices are still covered
 */
public class WebServiceData {

    private static final Map<Class<?>, WebServiceData> webServiceDataMap = new HashMap<>();

    private static boolean initialized = false;

//    enum WebserviceDomain {
//        ANALYSIS_PORTAL,
//        SERVICE_PORTAL, //a.k.a. upload portal
//
//        DATA_HUB,
//
//        MAINTENANCE_TASK_PLANNING,
//        INTERNAL
//    }

//   static {
//        webServiceDataMap.put(MaintenanceAgentImpl.class, new WebServiceData(MaintenanceAgentImpl.class, "Maintenance", "/service/rest/", INTERNAL));
//        webServiceDataMap.put(AnalysisBrowsingAgentImpl.class, new WebServiceData(AnalysisBrowsingAgentImpl.class, "Repository", "/repository/browse/", INTERNAL));
//        webServiceDataMap.put(AnalysisMetricDataApiImpl.class, new WebServiceData(AnalysisMetricDataApiImpl.class, "Metrics", "/repository/metrics/", WebserviceDomain.ANALYSIS_PORTAL));
//        webServiceDataMap.put(DashboardUserAgentImpl.class, new WebServiceData(DashboardUserAgentImpl.class, "User management", "/user/", INTERNAL));
//        webServiceDataMap.put(SbomFeedbackAgentImpl.class, new WebServiceData(SbomFeedbackAgentImpl.class, "Sbom feedback", "/sbom/feedback/", WebserviceDomain.ANALYSIS_PORTAL));
//        webServiceDataMap.put(AnalysisAgentImpl.class, new WebServiceData(AnalysisAgentImpl.class, "Analyzer", "/analyzer/rest/", WebserviceDomain.ANALYSIS_PORTAL));//configuration and execution
//        webServiceDataMap.put(LicenseServiceImpl.class, new WebServiceData(LicenseServiceImpl.class, "License", "/licence/rest/", WebserviceDomain.SERVICE_PORTAL));
//        webServiceDataMap.put(MonitorServiceImpl.class, new WebServiceData(MonitorServiceImpl.class, "Monitor", "/service/rest/", INTERNAL));
//        webServiceDataMap.put(DashboardAgentImpl.class, new WebServiceData(DashboardAgentImpl.class, "Dashboard", "/dashboard/", INTERNAL));
//        webServiceDataMap.put(FileManagerAgentImpl.class, new WebServiceData(FileManagerAgentImpl.class, "Upload", "/upload/rest/", WebserviceDomain.SERVICE_PORTAL));
//
//        webServiceDataMap.put(TaskPlanningAgentImpl.class, new WebServiceData(TaskPlanningAgentImpl.class, "Maintenance task planning", "/maintenance/planning/", MAINTENANCE_TASK_PLANNING));
//        webServiceDataMap.put(SecurityEventAgentImpl.class, new WebServiceData(SecurityEventAgentImpl.class, "Security event logging", "/maintenance/security/", MAINTENANCE_TASK_PLANNING));
//        webServiceDataMap.put(VulnerabilityServiceImpl.class, new WebServiceData(VulnerabilityServiceImpl.class, "Vulnerabilities", "/security/vulnerability/", WebserviceDomain.SERVICE_PORTAL));
//        webServiceDataMap.put(WeaknessAgentImpl.class, new WebServiceData(WeaknessAgentImpl.class, "Weaknesses", "/security/weakness/", WebserviceDomain.SERVICE_PORTAL));
//        webServiceDataMap.put(VulnerabilityAgentImpl.class, new WebServiceData(VulnerabilityAgentImpl.class, "Vulnerabilities", "/security/vulnerability/", WebserviceDomain.SERVICE_PORTAL));
//        webServiceDataMap.put(SbomAgentImpl.class, new WebServiceData(SbomAgentImpl.class, "Software bill of materials", "/security/sbom/", WebserviceDomain.SERVICE_PORTAL));
//
//        webServiceDataMap.put(AuthenticationAgentImpl.class, new WebServiceData(AuthenticationAgentImpl.class, "Authentication", "/auth/", INTERNAL));
//
//        webServiceDataMap.put(MetricDataApiImpl.class, new WebServiceData(MetricDataApiImpl.class, "Metrics", "/datahub/metrics/", WebserviceDomain.DATA_HUB));
//
//        webServiceDataMap.put(AssetAccessServiceImpl.class, new WebServiceData(AssetAccessServiceImpl.class, "Asset access management","/asset/",INTERNAL));
//
//        webServiceDataMap.put(AnalysisDownloadAgentImpl.class, new WebServiceData(AnalysisDownloadAgentImpl.class, "Analysis download facility","/repository/download/",INTERNAL));
//    }

    public static Collection<WebServiceData> getWebServices() {
        init();
        return webServiceDataMap.values();
    }

    public static WebServiceData getWebService(Class<?> webService) {
        init();
        return webServiceDataMap.get(webService);
    }

    private static void init() {
        if (initialized) {
            return;
        }

        for (WebServiceData webServiceData : webServiceDataMap.values()) {
            webServiceData.populate();
        }

        System.out.println(new LogEntry(Level.DEBUG, "initialized WebServiceData"));
        initialized = true;
    }

    private final List<EndpointData> endpointDataList = new ArrayList<>();
    private final Map<String, EndpointData> endpointDataByPath = new HashMap<>();

    private void populate() {
        for (Method method : implClass.getDeclaredMethods()) {
            Endpoint endpoint = method.getAnnotation(Endpoint.class);
            if (endpoint != null) {
                EndpointData endpointData = new EndpointData(method);
                endpointDataList.add(endpointData);
                endpointDataByPath.put(endpointData.getPath(), endpointData);
            }
        }

    }

    private final Class<?> implClass;
    private final String name;
    private final String path;
//    private final List<WebserviceDomain> domains;

    public WebServiceData(Class<?> implClass, String name, String path/*, WebserviceDomain ... domain*/) {
        this.implClass = implClass;
        this.name = name;
        this.path = path;
//        this.domains = Arrays.asList(domain);
    }

    public List<EndpointData> getEndpointDataList() {
        return endpointDataList;
    }

    public EndpointData getEndpointDataByPath(String path) {
        init();
        return endpointDataByPath.get(path);
    }

    public Class<?> getImplClass() {
        return implClass;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

//    public List<WebserviceDomain> getDomains() {
//        return domains;
//    }

    public boolean isSystemWebService() {
        return endpointDataList.stream().allMatch(EndpointData::isSystemEndpoint);
    }
}
