package org.ijsberg.iglu.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ijsberg.iglu.access.AccessManager;
import org.ijsberg.iglu.configuration.Assembly;
import org.ijsberg.iglu.configuration.Component;
import org.ijsberg.iglu.configuration.ConfigurationException;
import org.ijsberg.iglu.http.json.JsonData;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.http.RestSupport;
import org.ijsberg.iglu.util.http.ServletSupport;

import static org.ijsberg.iglu.rest.RequestPath.RequestMethod.*;
import static org.ijsberg.iglu.rest.RequestPath.ParameterType.*;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by J Meetsma on 22-8-2016.
 */
public class IgluRestServlet extends HttpServlet {

    private class RestMethodData {

        Method method;
        RequestPath requestPath;


        RestMethodData(RequestPath requestPath, Method method) {
            this.requestPath = requestPath;
            this.method = method;
            Class<?>[] parameterTypes = method.getParameterTypes();
            Class<?> returnType = method.getReturnType();
            if(requestPath.inputType() == VOID) {

            }
            else if(requestPath.inputType() != MAPPED) {
                if(parameterTypes.length != 1) {
                    throw new ConfigurationException("input type " + requestPath.inputType() + " requires 1 parameter");
                }
                if(requestPath.inputType() == PROPERTIES && !Properties.class.isAssignableFrom(parameterTypes[0])) {
                    throw new ConfigurationException("input type " + requestPath.inputType() + " requires 1 parameter of type Properties");
                }
                if(requestPath.inputType() == JSON && requestPath.method() != POST) {
                    throw new ConfigurationException("input type " + requestPath.inputType() + " requires invocation by method " + POST);
                }
            } else {
                if(requestPath.parameters().length != parameterTypes.length) {
                    throw new ConfigurationException("please define " + parameterTypes.length + " input parameters");
                }
            }
        }
    }

    private Assembly assembly;
    private String agentName;
    //FIXME allow for method overloading
    private Map<String, RestMethodData> invokeableMethods = new HashMap<>();

    public void setAssembly(Assembly assembly) {
        this.assembly = assembly;
    }

    public void setAgentType(String agentName, Class agentClass) {
        this.agentName = agentName;

        Method[] methods = agentClass.getDeclaredMethods();

        for(Method method : methods) {
            RequestPath requestPath = method.getAnnotation(RequestPath.class);
            if(requestPath != null) {
                String path = trimPath(requestPath.path());
                invokeableMethods.put(path, new RestMethodData(requestPath, method));
            }
        }
    }

    private static String readPostData(HttpServletRequest request) throws IOException {

        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        return buffer.toString();
    }

    private String trimPath(String inputPath) {
        String path = inputPath;
        if(path.startsWith("/")) {
            path = path.substring(1);
        }
        if(path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static Object[] getParameters(HttpServletRequest request, RestMethodData methodData) throws IOException {

        if(methodData.requestPath.inputType() == VOID) {
            return new Object[0];
        }
        RequestParameter[] declaredParameters = methodData.requestPath.parameters();
        if(methodData.requestPath.method() == POST) {

            String postData = readPostData(request);
//            RequestParameter parameter = declaredParameters[0];
            if(methodData.requestPath.inputType() == JSON) {
                ObjectMapper mapper = new ObjectMapper();
                Object obj = mapper.readValue(postData, methodData.method.getParameterTypes()[0]);
                return new Object[]{obj};
            }
            if(methodData.requestPath.inputType() == PROPERTIES) {
                Properties properties = ServletSupport.convertUrlEncodedData(postData);
                return new Object[]{properties};
            }
            if(methodData.requestPath.inputType() == MAPPED) {
                Properties properties = ServletSupport.convertUrlEncodedData(postData);
                Object[] result = new Object[declaredParameters.length];
                for(int i = 0; i < declaredParameters.length; i++) {
                    RequestParameter requestParameter = declaredParameters[i];
                    result[i] = properties.getProperty(requestParameter.name());
                }
                return result;
            }
            //STRING
            return new Object[]{postData};
        } else {
            if(methodData.requestPath.inputType() == PROPERTIES) {
                Properties properties = ServletSupport.getPropertiesFromRequest(request);
                return new Object[]{properties};
            }
        }
        //MAPPED
        Object[] result = new Object[declaredParameters.length];
        for(int i = 0; i < declaredParameters.length; i++) {
            RequestParameter requestParameter = declaredParameters[i];
            result[i] = request.getParameter(requestParameter.name());
        }
        return result;
    }


    public void service(HttpServletRequest servletRequest, HttpServletResponse response) throws IOException, ServletException {

        long start = System.currentTimeMillis();
        try {
            System.out.println(new LogEntry(this.getClass().getSimpleName() + " processing " + servletRequest.getPathInfo()));

            Object result = null;

            Component agentComponent = assembly.getCoreCluster().getInternalComponents().
                    get("AccessManager").getProxy(AccessManager.class).
                    getCurrentRequest().getSession(true).
                    getAgent(agentName);

            String pathInfo = servletRequest.getPathInfo();
            if (pathInfo == null) {
                pathInfo = "";
            }

            JsonData errorResult = null;

            String path = trimPath(pathInfo);
            RestMethodData restMethodData = invokeableMethods.get(path);
            if (restMethodData != null) {
                try {
                    result = agentComponent.invoke(restMethodData.method.getName(), getParameters(servletRequest, restMethodData));
                } catch (InvocationTargetException e) {
                    System.out.println(new LogEntry(Level.CRITICAL, "unable to invoke method " + restMethodData.method.getName(), e.getCause()));
                    if (e.getCause() instanceof RestException) {
                        errorResult = createErrorResponse((RestException) e.getCause());
                    } else {
                        throw new ServletException("unable to invoke method " + restMethodData.method.getName(), e);
                    }
                } catch (NoSuchMethodException e) {
                    System.out.println(new LogEntry(Level.CRITICAL, "unable to invoke method " + restMethodData.method.getName(), e));
                    throw new ServletException("unable to invoke method " + restMethodData.method.getName(), e);
                }
            } else {
                errorResult = RestSupport.createResponse(404, "no endpoint found for path " + path);
            }

            //TODO restMethodData:null leads to NPE

            ServletOutputStream out = response.getOutputStream();
            //FIXME
            response.setContentType("text/html");

            if (errorResult != null) {
                result = errorResult.toString();
                response.setStatus((Integer) errorResult.getAttribute("status"));
            } else if (restMethodData.requestPath.returnType() == JSON) {
                ObjectMapper mapper = new ObjectMapper();
                result = mapper.writeValueAsString(result);
            }

            out.println(result != null ? result.toString() :
                    (restMethodData.requestPath.returnType() == VOID ? "" : "null"));
        } catch(Exception e) {
            if(e instanceof ServletException) {
                throw e;
            } else {
                e.printStackTrace();
                System.out.println(new LogEntry(Level.CRITICAL, "unable to process request", e));
                try {
                    ServletOutputStream out = response.getOutputStream();
                    response.setContentType("text/html");
                    response.setStatus(500);
                    out.println("unexpected error");
                } catch (Exception ignore) {

                }
            }
        }
        System.out.println(new LogEntry(this.getClass().getSimpleName() + " processing " + servletRequest.getPathInfo() +
                " finished in " + (System.currentTimeMillis() - start) + " ms"));
    }


    public JsonData createErrorResponse(RestException e) {
        return RestSupport.createResponse(e.getHttpStatusCode(), e.getMessage(), e);
    }

}