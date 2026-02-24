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

        String pathInfo = ((HttpServletRequest) servletRequest).getPathInfo();
        filterChain.doFilter(servletRequest, servletResponse);
        if(!"/messages/latest".equals(pathInfo)) {
            System.out.println(new LogEntry(((HttpServletRequest)servletRequest).getPathInfo() + " " + ((HttpServletResponse)servletResponse).getStatus()));
            webTrafficMonitor.allowResponse((HttpServletRequest)servletRequest, (HttpServletResponse)servletResponse);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
