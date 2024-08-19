package org.ijsberg.iglu.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.ijsberg.iglu.rest.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EndpointData {

    private final String path;
    private final String method;
    private final List<String> parameters;
    private final String inputType;
    private final String secondInputType;
    private final String returnType;
    private final String description;
    private final List<String> permissions;
    private final boolean deprecated;
    private final boolean publicAccess;
    private final boolean systemEndpoint;

    @JsonIgnore
    private Endpoint endpoint;
    @JsonIgnore
    private Method javaMethod;

    public EndpointData(Method method) {
        endpoint = method.getAnnotation(Endpoint.class);
        javaMethod = method;
        this.path = endpoint.path();
        this.method = String.valueOf(endpoint.method());
        this.parameters = Arrays.stream(endpoint.parameters()).map(RequestParameter::name).collect(Collectors.toList());
        this.inputType = String.valueOf(endpoint.inputType());
        this.secondInputType = String.valueOf(endpoint.secondInputType());
        this.returnType = String.valueOf(endpoint.returnType());
        this.description = endpoint.description();

        RequireOneOrMorePermissions permissions = method.getAnnotation(RequireOneOrMorePermissions.class);
        this.permissions = permissions == null ? null : List.of(permissions.permission());

        Deprecated deprecated = method.getAnnotation(Deprecated.class);
        this.deprecated = deprecated != null;

        AllowPublicAccess allowPublicAccess = method.getAnnotation(AllowPublicAccess.class);
        this.publicAccess = allowPublicAccess != null;

        SystemEndpoint systemEndpoint = method.getAnnotation(SystemEndpoint.class);
        this.systemEndpoint = systemEndpoint != null;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public String getInputType() {
        return inputType;
    }

    public String getSecondInputType() {
        return secondInputType;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public boolean isSystemEndpoint() {
        return systemEndpoint;
    }

    @JsonIgnore
    public Endpoint getEndpoint() {
        return endpoint;
    }

    @JsonIgnore
    public Method getJavaMethod() {
        return javaMethod;
    }

}
