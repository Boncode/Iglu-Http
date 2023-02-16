package org.ijsberg.iglu.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.FatalException;
import org.ijsberg.iglu.access.AccessManager;
import org.ijsberg.iglu.access.Request;
import org.ijsberg.iglu.access.Session;
import org.ijsberg.iglu.access.User;
import org.ijsberg.iglu.configuration.Assembly;
import org.ijsberg.iglu.configuration.Component;
import org.ijsberg.iglu.configuration.ConfigurationException;
import org.ijsberg.iglu.http.json.JsonData;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.ResourceException;
import org.ijsberg.iglu.util.collection.ArraySupport;
import org.ijsberg.iglu.util.collection.CollectionSupport;
import org.ijsberg.iglu.util.http.RestSupport;
import org.ijsberg.iglu.util.http.ServletSupport;
import org.ijsberg.iglu.util.io.StreamSupport;
import org.ijsberg.iglu.util.mail.WebContentType;
import org.ijsberg.iglu.util.misc.StringSupport;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.ijsberg.iglu.access.Permissions.FULL_CONTROL;
import static org.ijsberg.iglu.http.client.Constants.ATTRIBUTE_SESSION_RESOLVED_BY_TOKEN;
import static org.ijsberg.iglu.http.client.Constants.HEADER_X_CSRF_TOKEN;
import static org.ijsberg.iglu.logging.Level.CRITICAL;
import static org.ijsberg.iglu.logging.Level.TRACE;
import static org.ijsberg.iglu.rest.Endpoint.ParameterType.*;
import static org.ijsberg.iglu.rest.Endpoint.RequestMethod.*;
import static org.ijsberg.iglu.util.http.HttpEncodingSupport.urlEncodeXSSRiskCharacters;
import static org.ijsberg.iglu.util.mail.WebContentType.HTML;
import static org.ijsberg.iglu.util.mail.WebContentType.JSON;

/**
 * Created by J Meetsma on 22-8-2016.
 */
public class IgluRestServlet extends HttpServlet {

    private AccessManager accessManager;

    public void setAccessManager(AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    private class RestMethodData {

        Method method;
        Endpoint endpoint;
        String[] requiredPermissions;
//        String requiredAccessRight;
        //rest method can either be invoked on:
        Component serviceComponent;
        //or by retrieving stateful agent by:
        String agentName;

        boolean allowScriptSnippets;

        boolean bypassCsrfCheck;

        RestMethodData(Endpoint endpoint, Method method, String agentName) {
            this.endpoint = endpoint;
            this.method = method;
            this.agentName = agentName;
            configure(endpoint, method);
        }

        RestMethodData(Endpoint endpoint, Method method, Component serviceComponent) {
            this.endpoint = endpoint;
            this.method = method;
            this.serviceComponent = serviceComponent;
            configure(endpoint, method);
        }

        private WebContentType getResponseContentType() {
            return endpoint.returnType();
        }

        private void configure(Endpoint endpoint, Method method) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            Class<?> returnType = method.getReturnType();
            Endpoint.ParameterType inputType = endpoint.inputType();
            allowScriptSnippets = method.isAnnotationPresent(AllowScriptSnippets.class);
            bypassCsrfCheck = method.isAnnotationPresent(BypassCsrfCheck.class);
            if(inputType == VOID) {

            } else if(inputType == RAW) {
                //TODO validation not exhaustive
                //RAW (icw PUT or POST) allows for additional parameters past
                if(endpoint.secondInputType() == MAPPED || endpoint.secondInputType() == FROM_PATH) {
                    if (endpoint.parameters().length + 1 /*raw bytes*/ != parameterTypes.length) {
                        throw new ConfigurationException("please define " + (parameterTypes.length - 1) + " extra input parameters method " + method.getName());
                    }
                }
            }
            else if(inputType == FROM_PATH) {
                //
            }
            else if(inputType == MAPPED) {
                if(endpoint.parameters().length != parameterTypes.length) {
                    throw new ConfigurationException("please define " + parameterTypes.length + " input parameters for method " + method.getName());
                }
            } else if(inputType == REQUEST_RESPONSE) {
                // it's up to the component to handle everything
            } else {
                if(parameterTypes.length != 1) {
                    throw new ConfigurationException("input type " + inputType + " requires exactly 1 parameter method " + method.getName());
                }
                if(inputType == PROPERTIES && !Properties.class.isAssignableFrom(parameterTypes[0])) {
                    throw new ConfigurationException("input type " + inputType + " requires 1 parameter of type Properties method " + method.getName());
                }
                if(inputType == JSON_POST && endpoint.method() != POST) {
                    throw new ConfigurationException("input type " + inputType + " requires invocation by method " + POST + " method " + method.getName());
                }
            }
            RequireOneOrMorePermissions requireOneOrMorePermissions = method.getAnnotation(RequireOneOrMorePermissions.class);
            if(requireOneOrMorePermissions != null) {
                List<String> requiredPermissionsTemp = new ArrayList<>(Arrays.asList(requireOneOrMorePermissions.permission()));
                if (!requiredPermissionsTemp.contains(FULL_CONTROL)){
                    requiredPermissionsTemp.add(FULL_CONTROL);
                }
                requiredPermissions = requiredPermissionsTemp.toArray(new String[0]);
            } else {
                AllowPublicAccess allowPublicAccess = method.getAnnotation(AllowPublicAccess.class);
                if(allowPublicAccess == null) {
                    requiredPermissions = new String[]{"x"};//AccessConstants.ADMIN_ROLE_NAME;
                }
            }
        }

        public void assertUserAuthorized() {
            if(requiredPermissions != null) {
                User user = accessManager.getCurrentRequest().getUser();
                if (user != null) {
                    System.out.println(new LogEntry(TRACE,"checking rights for " + user.getId() + " (" + CollectionSupport.format(user.getGroupNames(), ",") + ")"));
                    if (!user.hasOneOfRights(requiredPermissions)) {
                        throw new RestException("not authorized for endpoint " + this.endpoint.path(), 403);
                    }
                } else {
                    throw new RestException("not authenticated for endpoint " + this.endpoint.path(), 401);
                }
            }
        }

        public Component getComponent() {
            if(serviceComponent != null) {
                return serviceComponent;
            } else {
                try {
/*                    return assembly.getCoreCluster().getInternalComponents().
                            get("AccessManager").getProxy(AccessManager.class).
                            getCurrentRequest().getSession(true).
                            getAgent(agentName);
*/
                    return accessManager.getCurrentRequest().getSession(true).getAgent(agentName);
                } catch (NullPointerException e) {
                    System.out.println(assembly.getCoreCluster().getInternalComponents());
                    System.out.println(assembly.getCoreCluster().getInternalComponents().
                            get("AccessManager").getProxy(AccessManager.class));
                    System.out.println(assembly.getCoreCluster().getInternalComponents().
                            get("AccessManager").getProxy(AccessManager.class).
                            getCurrentRequest());
                    return null;
                }
            }
        }

        public String toString() {
            return endpoint.path() + " mapped to " + method.getName();
        }

        public boolean bypassCsrfCheck() {
            return bypassCsrfCheck;
        }
    }

    private Assembly assembly;
    private String agentName;
    private Class agentClass;
    //FIXME allow for method overloading

    //FIXME allow for http request method overloading
    private Map<String, RestMethodData> invokeableMethods = new HashMap<>();

    public void setAssembly(Assembly assembly) {
        this.assembly = assembly;
        accessManager = assembly.getCoreCluster().getFacade().getProxy("AccessManager", AccessManager.class);
    }

    public void setAgentType(String agentName, Class agentClass) {
        if(agentClass.isInterface()) {
            throw new ConfigurationException("agentClass " + agentClass.getSimpleName() + " cannot be an interface");
        }
        this.agentName = agentName;
        this.agentClass = agentClass;

    }

    public void setServiceComponent(Component serviceComponent, Class serviceClass) {

        if(serviceClass.isInterface()) {
            throw new ConfigurationException("serviceClass " + serviceClass.getSimpleName() + " cannot be an interface");
        }
        Method[] methods = serviceClass.getDeclaredMethods();

        for(Method method : methods) {
            Endpoint endpoint = method.getAnnotation(Endpoint.class);
            if(endpoint != null) {
                String path = trimPath(endpoint.path());
                addInvokeableMethod(path, new RestMethodData(endpoint, method, serviceComponent));
            }
        }
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        //TODO currently impl, should be in line with interface
        String agentClassInitParam = config.getInitParameter("agentClass");
        String agentNameInitParam = config.getInitParameter("agentName");
        if(agentClassInitParam != null && agentNameInitParam != null) {
            try {
                agentClass = Class.forName(agentClassInitParam);
            } catch (ClassNotFoundException e) {
                throw new ResourceException("Cannot load class " + agentClassInitParam);
            }
            agentName = agentNameInitParam;
        }

        if(agentClass != null) {
            Method[] methods = agentClass.getDeclaredMethods();
            for (Method method : methods) {
                Endpoint endpoint = method.getAnnotation(Endpoint.class);
                if (endpoint != null) {
                    String path = trimPath(endpoint.path());
                    addInvokeableMethod(path, new RestMethodData(endpoint, method, agentName));
                }
            }
        } else {
            System.out.println(new LogEntry(Level.DEBUG, "No agent class found for servlet: " + this.getServletInfo()));
        }
    }


    private void addInvokeableMethod(String path, RestMethodData restMethodData) {
        if(invokeableMethods.containsKey(path)) {
            throw new ConfigurationException("endpoint " + path + " not unique");
        }
        invokeableMethods.put(path, restMethodData);
    }

    private static byte[] readPostData(HttpServletRequest request) throws IOException {

        byte[] postData = StreamSupport.absorbInputStream(request.getInputStream());
        return postData;
    }

    private static String trimPath(String inputPath) {
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

        if(methodData.endpoint.inputType() == VOID || methodData.endpoint.inputType() == REQUEST_RESPONSE) {
            return new Object[0];
        }
        RequestParameter[] declaredParameters = methodData.endpoint.parameters();
        if(methodData.endpoint.method() == POST || methodData.endpoint.method() == PUT) {
            //@@@
            byte[] postData = readPostData(request);
 //           RequestParameter parameter = declaredParameters[0];

            if(methodData.endpoint.inputType() == JSON_POST) {
                ObjectMapper mapper = new ObjectMapper();
                Object obj = null;
                try {
                    obj = mapper.readValue(postData, methodData.method.getParameterTypes()[0]);
                } catch(IOException e) {
                    throw new FatalException("unable to read post data '" + postData + "' for parameter type " + methodData.method.getParameterTypes()[0] + " while invoking " + methodData.endpoint, e);
                }
                return new Object[]{obj};
            }
            if(methodData.endpoint.inputType() == PROPERTIES) {
                Properties properties = ServletSupport.convertUrlEncodedData(new String(postData));
                return new Object[]{properties};
            }
            if(methodData.endpoint.inputType() == RAW) {
                Object[] additionalParams = getInputObjectsFromRequest(request, methodData, methodData.endpoint.secondInputType());
                Object[] result = new Object[1 + additionalParams.length];
                System.arraycopy(additionalParams, 0, result, 1, additionalParams.length);
                result[0] = postData;
                return result;
            }
            if(methodData.endpoint.inputType() == STRING) {
                return new Object[]{new String(postData)};
            }
            if(methodData.endpoint.inputType() == MAPPED) {
                return getObjectsFromQueryString(declaredParameters, request.getQueryString());
            }
            //STRING
            return new Object[]{new String(postData)};
        } else { //GET
            Object[] result = getInputObjectsFromRequest(request, methodData, methodData.endpoint.inputType());
            //System.out.println("initial input: " + ArraySupport.format(result, ","));
            if(methodData.endpoint.secondInputType() != null) {

                Object[] additionalInput = getInputObjectsFromRequest(request, methodData, methodData.endpoint.secondInputType());
               //System.out.println("additional input: " + ArraySupport.format(additionalInput, ","));

                result = ArraySupport.join(result, additionalInput);
                //System.out.println("result input: " + ArraySupport.format(result, ","));
            }
            return result;
        }
    }

    private static Object[] getObjectsFromQueryString(RequestParameter[] declaredParameters, String queryString) throws UnsupportedEncodingException {
        Properties properties = ServletSupport.convertUrlEncodedData(queryString);
        Object[] result = new Object[declaredParameters.length];
        for(int i = 0; i < declaredParameters.length; i++) {
            RequestParameter requestParameter = declaredParameters[i];
            result[i] = properties.getProperty(requestParameter.name());
        }
        return result;
    }

    private static Object[] getInputObjectsFromRequest(HttpServletRequest request, RestMethodData methodData, Endpoint.ParameterType parameterType) throws UnsupportedEncodingException {
        if(parameterType == FROM_PATH) {
            String path = trimPath(request.getPathInfo()).substring(methodData.endpoint.path().length());
            path = trimPath(path);
            if("".equals(path)) {
                return new Object[0];
            }
            //System.out.println("PATH: " + path);
            return path.split("/");
        }
        if(parameterType == PROPERTIES) {
            Properties properties = ServletSupport.getPropertiesFromRequest(request);
            return new Object[]{properties};
        }
        if(parameterType == STRING) {
            Properties properties = ServletSupport.getPropertiesFromRequest(request);
            if(properties.size() > 0) {
                return new Object[]{properties.getProperty(properties.stringPropertyNames().iterator().next())};
            }
            return new Object[]{null};
        }
        if(parameterType == MAPPED) {
            if(request.getQueryString() != null) {
                RequestParameter[] declaredParameters = methodData.endpoint.parameters();
                Object[] result = getObjectsFromQueryString(declaredParameters, request.getQueryString());
                return result;
            }
        }
        //VOID
        return new Object[0];
    }

    private void checkCsrfToken(HttpServletRequest request, RestMethodData restMethodData) {
        if(restMethodData.endpoint.method() != GET && !restMethodData.bypassCsrfCheck()) {
            Request userRequest = accessManager.getCurrentRequest();
            if (userRequest.hasSession()) {
                Session session = userRequest.getSession(false);

//                System.out.println("SessionResolvedByToken:" + Boolean.parseBoolean("" + session.getAttribute(ATTRIBUTE_SESSION_RESOLVED_BY_TOKEN)));
//                System.out.println("X-CSRF-Token http-header:" + request.getHeader(HEADER_X_CSRF_TOKEN));
//                System.out.println("X-CSRF-Token session:" + session.getAttribute(HEADER_X_CSRF_TOKEN));

                if (!Boolean.parseBoolean("" + session.getAttribute(ATTRIBUTE_SESSION_RESOLVED_BY_TOKEN))) {
                    String csrfTokenFromSession = (String) session.getAttribute(HEADER_X_CSRF_TOKEN);
                    if (csrfTokenFromSession != null) {
                        String csrfTokenFromHeader = request.getHeader(HEADER_X_CSRF_TOKEN);
                        if (!csrfTokenFromSession.equals(csrfTokenFromHeader)) {
                            System.out.println(new LogEntry(CRITICAL, "could not verify request origin for request " + request.getPathInfo()));
                            throw new RestException("could not verify request origin", 401);
                        }
                    }
                }
            }
        }
    }

    public void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException {

        long start = System.currentTimeMillis();


        RestMethodData restMethodData = null;


        try {
            System.out.println(new LogEntry(TRACE, this.getClass().getSimpleName() + " processing " + servletRequest.getPathInfo()));

            Object result = null;


            String pathInfo = servletRequest.getPathInfo();
            if (pathInfo == null) {
                pathInfo = "";
            }

            JsonData errorResult = null;

            String path = urlEncodeXSSRiskCharacters(trimPath(pathInfo));
            restMethodData = getRestMethodData(path);

            checkCsrfToken(servletRequest, restMethodData);

            WebContentType contentType = HTML;

            if (restMethodData != null) {
                if(restMethodData.method.getAnnotation(Deprecated.class) != null) {
                    System.out.println(new LogEntry(Level.CRITICAL, "Deprecated endpoint called: " + path));
                }

                contentType = restMethodData.getResponseContentType();
                restMethodData.assertUserAuthorized();
                try {
                    if(restMethodData.endpoint.inputType() == REQUEST_RESPONSE) {
//                    if(contentType == EVENT_STREAM) {
                        result = restMethodData.getComponent().invoke(restMethodData.method.getName(), servletRequest, servletResponse);
                        return;
                    } else {
                        Object[] parameters = getParameters(servletRequest, restMethodData);
                        checkInput(parameters);
                        result = restMethodData.getComponent().invoke(restMethodData.method.getName(), parameters);
                    }
                } catch (InvocationTargetException e) {
                    System.out.println(new LogEntry(Level.CRITICAL, "unable to invoke method " + restMethodData.method.getName(), e.getCause()));
                    if (e.getCause() instanceof RestException) {
                        errorResult = createErrorResponse((RestException) e.getCause());
                    } else {
                        throw new FatalException("unable to invoke method " + restMethodData.method.getName(), e);
                    }
                } catch (NoSuchMethodException e) {
                    System.out.println(new LogEntry(Level.CRITICAL, "unable to invoke method " + restMethodData.method.getName(), e));
                    throw new FatalException("unable to invoke method " + restMethodData.method.getName(), e);
                }
            } else {
                errorResult = RestSupport.createResponse(404, "no endpoint found for path " + path);
            }

            result = purgeResponse(result);



            //filterResult

            //TODO restMethodData:null leads to NPE

            ServletOutputStream out = servletResponse.getOutputStream();
            servletResponse.setContentType(contentType.getContentType());

            if (errorResult != null) {
                result = errorResult.toString();
                servletResponse.setStatus((Integer) errorResult.getAttribute("status"));
            } else if (!(result instanceof String) && restMethodData.endpoint.returnType() == JSON) {
                ObjectMapper mapper = new ObjectMapper();
                result = mapper.writeValueAsString(result);
            }

            out.print(result != null ? purgeXSS(result.toString(), restMethodData) : "");
                    //TODO return type JSON
                    //(restMethodData.requestPath.returnType() == VOID ? "" : "null"));
        } catch(Exception e) {
            handleException(restMethodData, servletResponse, e);
        }
        System.out.println(new LogEntry(TRACE, this.getClass().getSimpleName() + " processing " + servletRequest.getPathInfo() +
                " finished in " + (System.currentTimeMillis() - start) + " ms"));
    }

    public static String purgeXSS(String stringResponse, RestMethodData restMethodData) {
        int start = 0;
        int index = 0;
        if(!restMethodData.allowScriptSnippets && stringResponse.indexOf('<', start) != -1) {
            String stringResponseLowerCase = stringResponse.toLowerCase();
            if(stringResponseLowerCase.contains("script")) {
                stringResponseLowerCase = StringSupport.replaceAll(stringResponseLowerCase, new String[]{"\t", " "}, new String[]{"",""});
                while((index = stringResponseLowerCase.indexOf("</script>", start)) > 0) {
                    start++;
                    if(stringResponseLowerCase.charAt(index - 1) != '>') {
                        System.out.println(new LogEntry(CRITICAL, "script entry not empty for " + restMethodData.endpoint.path(), stringResponseLowerCase.substring(0, index + 50)));
                        return StringSupport.replaceAll(stringResponse, new String[]{"<",">"},new String[]{"&lt;","&gt;"});
                    }
                }
            }
        }
        return stringResponse;
        //return stringResponse.replace("<", "&lt;AAP");
    }

    public void handleException(RestMethodData restMethodData, HttpServletResponse response, Exception e) throws IOException {
        try {
            ServletOutputStream out = response.getOutputStream();
            response.setContentType("text/plain");
            if(e instanceof RestException) {
                System.out.println(new LogEntry(Level.DEBUG, "process request failed with predictable error", e));
                respondWithError(response, restMethodData, ((RestException) e).getHttpStatusCode(), e.getMessage());
            } else {
                System.out.println(new LogEntry(Level.CRITICAL, "processing request failed with server error", e));
                respondWithError(response, restMethodData, 500, "unexpected error");
            }
        } catch (Exception excHandlingException) {
            System.out.println(new LogEntry(Level.CRITICAL, "exception handling failed", excHandlingException));
        }
    }

    private void respondWithError(HttpServletResponse response, RestMethodData restMethodData, int httpStatusCode, String message) throws IOException {
        ServletOutputStream out = response.getOutputStream();
        response.setStatus(httpStatusCode);
        if(restMethodData != null && restMethodData.getResponseContentType() == JSON) {
            out.print(RestSupport.createResponse(httpStatusCode, message).toString());
        } else {
            out.print(message);
        }

    }

    public RestMethodData getRestMethodData(String path) {
        RestMethodData returnValue = invokeableMethods.get(path);
        if(returnValue == null) {
            List<String> pathParts = StringSupport.split(path, "/");
            while(returnValue == null && pathParts.size() > 1) {
                pathParts.remove(pathParts.size() - 1);
//                System.out.println("trying " + CollectionSupport.format(pathParts, "/"));
                returnValue = invokeableMethods.get(CollectionSupport.format(pathParts, "/"));
            }
        }
//        System.out.println(new LogEntry(TRACE, "found " + returnValue));
        return returnValue;
    }

    private void checkInput(Object[] parameters) {
/*        for(Object parameter : parameters) {
            if(parameter instanceof TenantAwareInput) {
                System.out.println(new LogEntry("Found TenantAwareInput, tenant: " + ((TenantAwareInput)parameter).getTenantId()));
            }
        }*/
    }

    private Object purgeResponse(Object result) {
/*        User user = accessManager.getCurrentRequest().getUser();
        if(user != null) {
            System.out.println(new LogEntry("purging result for user " + user.getId() + " : " + user.getGroup()));
        }
*/
        return result;
    }

    public JsonData createErrorResponse(RestException e) {
        return RestSupport.createResponse(e.getHttpStatusCode(), e.getMessage(), e);
    }

}