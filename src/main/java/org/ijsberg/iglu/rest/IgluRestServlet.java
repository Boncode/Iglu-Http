package org.ijsberg.iglu.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ijsberg.iglu.FatalException;
import org.ijsberg.iglu.access.AccessConstants;
import org.ijsberg.iglu.access.AccessManager;
import org.ijsberg.iglu.access.User;
import org.ijsberg.iglu.configuration.Assembly;
import org.ijsberg.iglu.configuration.Component;
import org.ijsberg.iglu.configuration.ConfigurationException;
import org.ijsberg.iglu.http.json.JsonData;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.collection.ArraySupport;
import org.ijsberg.iglu.util.collection.CollectionSupport;
import org.ijsberg.iglu.util.http.RestSupport;
import org.ijsberg.iglu.util.http.ServletSupport;
import org.ijsberg.iglu.util.io.StreamSupport;
import org.ijsberg.iglu.util.mail.WebContentType;
import org.ijsberg.iglu.util.misc.StringSupport;

import static org.ijsberg.iglu.logging.Level.TRACE;
import static org.ijsberg.iglu.rest.RequestPath.RequestMethod.*;
import static org.ijsberg.iglu.rest.RequestPath.ParameterType.*;
import static org.ijsberg.iglu.util.http.HttpEncodingSupport.urlEncode;
import static org.ijsberg.iglu.util.http.HttpEncodingSupport.urlEncodeXSSRiskCharacters;
import static org.ijsberg.iglu.util.mail.WebContentType.*;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
        RequestPath requestPath;
        String requiredRole;
        //rest method can either be invoked on:
        Component serviceComponent;
        //or by retrieving stateful agent by:
        String agentName;

        RestMethodData(RequestPath requestPath, Method method, String agentName) {
            this.requestPath = requestPath;
            this.method = method;
            this.agentName = agentName;
            configure(requestPath, method);
        }

        RestMethodData(RequestPath requestPath, Method method, Component serviceComponent) {
            this.requestPath = requestPath;
            this.method = method;
            this.serviceComponent = serviceComponent;
            configure(requestPath, method);
        }

        private WebContentType getResponseContentType() {
            return requestPath.returnType();
        }

        private void configure(RequestPath requestPath, Method method) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            Class<?> returnType = method.getReturnType();
            RequestPath.ParameterType inputType = requestPath.inputType();
            if(inputType == VOID) {

            } else if(inputType == RAW) {
                //TODO validation not exhaustive
                //RAW (icw PUT or POST) allows for additional parameters past
                if(requestPath.secondInputType() == MAPPED || requestPath.secondInputType() == FROM_PATH) {
                    if (requestPath.parameters().length + 1 /*raw bytes*/ != parameterTypes.length) {
                        throw new ConfigurationException("please define " + (parameterTypes.length - 1) + " extra input parameters method " + method.getName());
                    }
                }
            }
            else if(inputType == FROM_PATH) {
                //
            }
            else if(inputType == MAPPED) {
                if(requestPath.parameters().length != parameterTypes.length) {
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
                if(inputType == JSON_POST && requestPath.method() != POST) {
                    throw new ConfigurationException("input type " + inputType + " requires invocation by method " + POST + " method " + method.getName());
                }
            }
            AssertUserRole assertUserRole = method.getAnnotation(AssertUserRole.class);
            if(assertUserRole != null) {
                requiredRole = assertUserRole.role();
            } else {
                AllowPublicAccess allowPublicAccess = method.getAnnotation(AllowPublicAccess.class);
                if(allowPublicAccess == null) {
                    requiredRole = AccessConstants.ADMIN_ROLE_NAME;
                }
            }
        }

        public void assertUserAuthorized() {
            if(requiredRole != null) {
                User user = accessManager.getCurrentRequest().getUser();
                if (user != null) {
                    System.out.println(new LogEntry(TRACE,"checking rights for " + user.getId() + (user.getGroup() != null ? " : " + user.getGroup().getName() : "")));
                    if (!user.hasRole(requiredRole)) {
                        throw new RestException("not authorized for endpoint " + this.requestPath.path(), 403);
                    }
                } else {
                    throw new RestException("not authenticated for endpoint " + this.requestPath.path(), 401);
                }
            }
        }

        public Component getComponent() {
            if(serviceComponent != null) {
                return serviceComponent;
            } else {
                return assembly.getCoreCluster().getInternalComponents().
                        get("AccessManager").getProxy(AccessManager.class).
                        getCurrentRequest().getSession(true).
                        getAgent(agentName);
            }
        }

        public String toString() {
            return requestPath.path() + " mapped to " + method.getName();
        }

    }

    private Assembly assembly;
    private String agentName;
    //FIXME allow for method overloading
    private Map<String, RestMethodData> invokeableMethods = new HashMap<>();

    public void setAssembly(Assembly assembly) {
        this.assembly = assembly;
        accessManager = assembly.getCoreCluster().getFacade().getProxy("AccessManager", AccessManager.class);
    }

    public void setAgentType(String agentName, Class agentClass) {
        this.agentName = agentName;

        Method[] methods = agentClass.getDeclaredMethods();

        for(Method method : methods) {
            RequestPath requestPath = method.getAnnotation(RequestPath.class);
            if(requestPath != null) {
                String path = trimPath(requestPath.path());
                addInvokeableMethod(path, new RestMethodData(requestPath, method, agentName));
            }
        }
    }

    public void setServiceComponent(Component serviceComponent, Class serviceClass) {
        Method[] methods = serviceClass.getDeclaredMethods();

        for(Method method : methods) {
            RequestPath requestPath = method.getAnnotation(RequestPath.class);
            if(requestPath != null) {
                String path = trimPath(requestPath.path());
                addInvokeableMethod(path, new RestMethodData(requestPath, method, serviceComponent));
            }
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

        if(methodData.requestPath.inputType() == VOID || methodData.requestPath.inputType() == REQUEST_RESPONSE) {
            return new Object[0];
        }
        RequestParameter[] declaredParameters = methodData.requestPath.parameters();
        if(methodData.requestPath.method() == POST || methodData.requestPath.method() == PUT) {
            //@@@
            byte[] postData = readPostData(request);
 //           RequestParameter parameter = declaredParameters[0];

            if(methodData.requestPath.inputType() == JSON_POST) {
                ObjectMapper mapper = new ObjectMapper();
                Object obj = null;
                try {
                    obj = mapper.readValue(postData, methodData.method.getParameterTypes()[0]);
                } catch(IOException e) {
                    throw new FatalException("unable to read post data '" + postData + "' for parameter type " + methodData.method.getParameterTypes()[0] + " while invoking " + methodData.requestPath, e);
                }
                return new Object[]{obj};
            }
            if(methodData.requestPath.inputType() == PROPERTIES) {
                Properties properties = ServletSupport.convertUrlEncodedData(new String(postData));
                return new Object[]{properties};
            }
            if(methodData.requestPath.inputType() == RAW) {
                Object[] additionalParams = getInputObjectsFromRequest(request, methodData, methodData.requestPath.secondInputType());
                Object[] result = new Object[1 + additionalParams.length];
                System.arraycopy(additionalParams, 0, result, 1, additionalParams.length);
                result[0] = postData;
                return result;
            }
            if(methodData.requestPath.inputType() == STRING) {
                return new Object[]{new String(postData)};
            }
            if(methodData.requestPath.inputType() == MAPPED) {
                Object[] result = getObjectsFromQueryString(declaredParameters, new String(postData));
                return result;
            }
            //STRING
            return new Object[]{new String(postData)};
        } else { //GET
            Object[] result = getInputObjectsFromRequest(request, methodData, methodData.requestPath.inputType());
            //System.out.println("initial input: " + ArraySupport.format(result, ","));
            if(methodData.requestPath.secondInputType() != null) {

                Object[] additionalInput = getInputObjectsFromRequest(request, methodData, methodData.requestPath.secondInputType());
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

    private static Object[] getInputObjectsFromRequest(HttpServletRequest request, RestMethodData methodData, RequestPath.ParameterType parameterType) throws UnsupportedEncodingException {
        if(parameterType == FROM_PATH) {
            String path = trimPath(request.getPathInfo()).substring(methodData.requestPath.path().length());
            path = trimPath(path);
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
                RequestParameter[] declaredParameters = methodData.requestPath.parameters();
                Object[] result = getObjectsFromQueryString(declaredParameters, request.getQueryString());
                return result;
            }
        }
        //VOID
        return new Object[0];
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

            WebContentType contentType = HTML;

            if (restMethodData != null) {
                contentType = restMethodData.getResponseContentType();
                restMethodData.assertUserAuthorized();
                try {
                    if(restMethodData.requestPath.inputType() == REQUEST_RESPONSE) {
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
            } else if (!(result instanceof String) && restMethodData.requestPath.returnType() == JSON) {
                ObjectMapper mapper = new ObjectMapper();
                result = mapper.writeValueAsString(result);
            }

            out.print(result != null ? result.toString() : "");
                    //TODO return type JSON
                    //(restMethodData.requestPath.returnType() == VOID ? "" : "null"));
        } catch(Exception e) {
            handleException(restMethodData, servletResponse, e);
        }
        System.out.println(new LogEntry(TRACE, this.getClass().getSimpleName() + " processing " + servletRequest.getPathInfo() +
                " finished in " + (System.currentTimeMillis() - start) + " ms"));
    }

    public void handleException(RestMethodData restMethodData, HttpServletResponse response, Exception e) throws IOException {
        System.out.println(new LogEntry(Level.CRITICAL, "unable to process request", e));
        try {
            ServletOutputStream out = response.getOutputStream();
            response.setContentType("text/plain");
            if(e instanceof RestException) {
//                response.setStatus(((RestException) e).getHttpStatusCode());
                respondWithError(response, restMethodData, ((RestException) e).getHttpStatusCode(), e.getMessage());
//                out.println(e.getMessage());
            } else {
//                response.setStatus(500);
                respondWithError(response, restMethodData, 500, "unexpected error");
//                out.println("unexpected error");
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