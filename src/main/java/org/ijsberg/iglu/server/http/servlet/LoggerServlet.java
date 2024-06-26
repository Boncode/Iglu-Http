package org.ijsberg.iglu.server.http.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.logging.Logger;
import org.ijsberg.iglu.util.misc.StringSupport;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LoggerServlet extends HttpServlet implements Logger {

    private int logLevelOrdinal = Level.DEBUG.ordinal();
    private List<LogEntry> logEntryQueue = new ArrayList();

    private static final int MAX = 100;

    /**
     * @param conf
     * @throws ServletException
     */
    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);
        String logLevel = conf.getInitParameter("log_level");
        if(logLevel != null) {
            logLevelOrdinal = Level.valueOf(logLevel).ordinal();
        }
    }

    public void service(HttpServletRequest servletRequest, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");

        out.println("<div style=\"display: flex; flex-direction: column; font-family: monospace;\">");
        List<LogEntry> logEntries = new ArrayList<>(logEntryQueue);
        for(LogEntry logEntry : logEntries) {
            String entryString = logEntry.toString();
            entryString = StringSupport.replaceAll(entryString, "\n", "<br>");
            out.println("<span class=\"LogEntry\">" + entryString + "</span>");
        }
        out.println("<div id=\"end_of_log\"></div>");
        out.println("</div>");
    }

    @Override
    public void log(LogEntry entry) {
        if (entry.getLevel().ordinal() >= logLevelOrdinal) {
            if (logEntryQueue.size() > MAX) {
                logEntryQueue.remove(0);
            }
            logEntryQueue.add(entry);
        }
    }

    @Override
    public void addAppender(Logger appender) {
    }

    @Override
    public void removeAppender(Logger appender) {
    }

    @Override
    public int getLogLevelOrdinal() {
        return logLevelOrdinal;
    }

    @Override
    public Properties getProperties() {
        return null;
    }
}
