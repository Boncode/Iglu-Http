package org.ijsberg.iglu.server.http.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.server.http.WebTrafficMonitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WebTrafficFilter implements Filter {

    private WebTrafficMonitor webTrafficMonitor;
    private List<String> pathsToIgnore = new ArrayList<>();

    public void setWebTrafficMonitor(WebTrafficMonitor webTrafficMonitor) {
        this.webTrafficMonitor = webTrafficMonitor;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        String pathInfo = ((HttpServletRequest) servletRequest).getPathInfo();
        if(webTrafficMonitor.allowRequest((HttpServletRequest)servletRequest, (HttpServletResponse)servletResponse)) {
            filterChain.doFilter(servletRequest, servletResponse);
            if(!shouldIgnorePath(pathInfo)) {
                webTrafficMonitor.checkResponse((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
            }
        } else {
            /*
            //System.out.println("Web traffic not allowed");
            ((HttpServletResponse) servletResponse).sendError(418);
            */
        }
    }
    private boolean shouldIgnorePath(String pathInfo) {
        for(String path : pathsToIgnore) {
            if(pathInfo.endsWith(path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
        String pathsToIgnoreValue = filterConfig.getInitParameter("pathsToIgnore");
        if(pathsToIgnoreValue != null) {
            pathsToIgnore = Arrays.asList(pathsToIgnoreValue.split(","));
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
