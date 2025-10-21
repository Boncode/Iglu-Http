package org.ijsberg.iglu.rest.model;

import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.rest.Endpoint;
import org.ijsberg.iglu.util.ResourceException;

import java.lang.reflect.Method;
import java.util.*;


public class WebServiceData {

    private static final Map<Class<?>, WebServiceData> webServiceDataMap = new HashMap<>();

    // todo probably we want to throw when exists, but I can imagine this will lead to false positive situations
    // todo it would be nice if Assemblies could automatically register any IgluRestServlet kind, almost possible
    public static void register(WebServiceData webServiceData) {
        if(webServiceDataMap.containsKey(webServiceData.implClass)) {
            System.out.println(new LogEntry(Level.VERBOSE, "WebServiceData already registered for: " + webServiceData.name));
        }
        webServiceDataMap.put(webServiceData.implClass, webServiceData);
    }

    public static Collection<WebServiceData> getWebServices() {
        return webServiceDataMap.values();
    }

    public static WebServiceData getWebService(Class<?> webService) {
        return webServiceDataMap.get(webService);
    }


    private final List<EndpointData> endpointDataList = new ArrayList<>();
    private final Map<String, EndpointData> endpointDataByPath = new HashMap<>();

    private void populate() {
        Method[] methods;
        try {
            methods = implClass.getDeclaredMethods();
        } catch (Throwable t) {
            throw new ResourceException("cannot get declared methods from " + implClass.getName(), t);
        }
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

    public WebServiceData(Class<?> implClass, String name, String path) {
        this.implClass = implClass;
        this.name = name;
        this.path = path;
        populate();
    }

    public List<EndpointData> getEndpointDataList() {
        return endpointDataList;
    }

    public EndpointData getEndpointDataByPath(String path) {
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

    public boolean isSystemWebService() {
        return endpointDataList.stream().allMatch(EndpointData::isSystemEndpoint);
    }
}
