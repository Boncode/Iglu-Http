package org.ijsberg.iglu.server.http.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.server.http.WebTrafficMonitor;

import java.io.IOException;

public class WebTrafficFilter implements Filter {

    private WebTrafficMonitor webTrafficMonitor;

    public void setWebTrafficMonitor(WebTrafficMonitor webTrafficMonitor) {
        this.webTrafficMonitor = webTrafficMonitor;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        /*if(!webTrafficMonitor.allowRequest((HttpServletRequest)servletRequest, (HttpServletResponse)servletResponse)) {
            //System.out.println("Web traffic not allowed");
            ((HttpServletResponse) servletResponse).sendError(418);
            return;
        }*/

        String pathInfo = ((HttpServletRequest) servletRequest).getPathInfo();
        filterChain.doFilter(servletRequest, servletResponse);
        if(     webTrafficMonitor.allowRequest((HttpServletRequest)servletRequest, (HttpServletResponse)servletResponse)
                && !"/messages/latest".equals(pathInfo)) {
            //System.out.println(new LogEntry(((HttpServletRequest)servletRequest).getPathInfo() + " " + ((HttpServletResponse)servletResponse).getStatus()));
            webTrafficMonitor.allowResponse((HttpServletRequest)servletRequest, (HttpServletResponse)servletResponse);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //todo get paths to ignore
        Filter.super.init(filterConfig);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
