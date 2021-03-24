package org.ijsberg.iglu.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.util.StringUtil;
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
import org.ijsberg.iglu.usermanagement.multitenancy.model.TenantAwareInput;
import org.ijsberg.iglu.util.collection.CollectionSupport;
import org.ijsberg.iglu.util.http.RestSupport;
import org.ijsberg.iglu.util.http.ServletSupport;
import org.ijsberg.iglu.util.io.StreamSupport;
import org.ijsberg.iglu.util.misc.StringSupport;

import static org.ijsberg.iglu.logging.Level.TRACE;
import static org.ijsberg.iglu.rest.RequestPath.RequestMethod.*;
import static org.ijsberg.iglu.rest.RequestPath.ParameterType.*;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
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

        private String getResponseContentType() {
            return requestPath.responseContentType().responseTypeValue;
        }

        private void configure(RequestPath requestPath, Method method) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            Class<?> returnType = method.getReturnType();
            if(requestPath.inputType() == VOID) {

            } else if(requestPath.inputType() == RAW) {
                //TODO validation not exhaustive
                //RAW (icw PUT or POST) allows for additional parameters past
                if(requestPath.secondInputType() == MAPPED || requestPath.secondInputType() == FROM_PATH) {
                    if (requestPath.parameters().length + 1 /*raw bytes*/ != parameterTypes.length) {
                        throw new ConfigurationException("please define " + (parameterTypes.length - 1) + " extra input parameters");
                    }
                }
            }
            else if(requestPath.inputType() == MAPPED || requestPath.inputType() == FROM_PATH) {
                if(requestPath.parameters().length != parameterTypes.length) {
                    throw new ConfigurationException("please define " + parameterTypes.length + " input parameters");
                }
            } else {
                if(parameterTypes.length != 1) {
                    throw new ConfigurationException("input type " + requestPath.inputType() + " requires exactly 1 parameter");
                }
                if(requestPath.inputType() == PROPERTIES && !Properties.class.isAssignableFrom(parameterTypes[0])) {
                    throw new ConfigurationException("input type " + requestPath.inputType() + " requires 1 parameter of type Properties");
                }
                if(requestPath.inputType() == JSON && requestPath.method() != POST) {
                    throw new ConfigurationException("input type " + requestPath.inputType() + " requires invocation by method " + POST);
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
                    System.out.println(new LogEntry("checking rights for " + user.getId() + (user.getGroup() != null ? " : " + user.getGroup().getName() : "")));
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

/*        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        return buffer.toString();*/
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

        if(methodData.requestPath.inputType() == VOID) {
            return new Object[0];
        }
        RequestParameter[] declaredParameters = methodData.requestPath.parameters();
        if(methodData.requestPath.method() == POST || methodData.requestPath.method() == PUT) {
            //@@@
            byte[] postData = readPostData(request);
 //           RequestParameter parameter = declaredParameters[0];

            if(methodData.requestPath.inputType() == JSON) {
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
                Properties properties = ServletSupport.convertUrlEncodedData(new String(postData));
                Object[] result = new Object[declaredParameters.length];
                for(int i = 0; i < declaredParameters.length; i++) {
                    RequestParameter requestParameter = declaredParameters[i];
                    result[i] = properties.getProperty(requestParameter.name());
                }
                return result;
            }
            //STRING
            return new Object[]{new String(postData)};
        } else {
            return getInputObjectsFromRequest(request, methodData, methodData.requestPath.inputType());
        }
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
                Properties properties = ServletSupport.convertUrlEncodedData(request.getQueryString());
                Object[] result = new Object[declaredParameters.length];
                for (int i = 0; i < declaredParameters.length; i++) {
                    RequestParameter requestParameter = declaredParameters[i];
                    result[i] = properties.getProperty(requestParameter.name());
                }
                return result;
            }
        }
        //VOID
        return new Object[0];
    }


    public void service(HttpServletRequest servletRequest, HttpServletResponse response) throws IOException, ServletException {

        long start = System.currentTimeMillis();
        try {
            System.out.println(new LogEntry(TRACE, this.getClass().getSimpleName() + " processing " + servletRequest.getPathInfo()));

            Object result = null;


            String pathInfo = servletRequest.getPathInfo();
            if (pathInfo == null) {
                pathInfo = "";
            }

            JsonData errorResult = null;

            String path = trimPath(pathInfo);
            RestMethodData restMethodData = getRestMethodData(path);

            String contentType = "text/html";

            if (restMethodData != null) {
                contentType = restMethodData.getResponseContentType();
                restMethodData.assertUserAuthorized();
                try {
                    Object[] parameters = getParameters(servletRequest, restMethodData);
                    checkInput(parameters);
                    result = restMethodData.getComponent().invoke(restMethodData.method.getName(), parameters);
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

            ServletOutputStream out = response.getOutputStream();
            response.setContentType(contentType);

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
                //TODO controlled response
                throw e;
            } else {
                System.out.println(new LogEntry(Level.CRITICAL, "unable to process request", e));
                try {
                    ServletOutputStream out = response.getOutputStream();
                    response.setContentType("text/plain");
                    if(e instanceof RestException) {
                        response.setStatus(((RestException)e).getHttpStatusCode());
                        out.println(e.getMessage());
                    } else {
                        response.setStatus(500);
                        out.println("unexpected error");
                    }
                } catch (Exception ignore) {

                }
            }
        }
        System.out.println(new LogEntry(TRACE, this.getClass().getSimpleName() + " processing " + servletRequest.getPathInfo() +
                " finished in " + (System.currentTimeMillis() - start) + " ms"));
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