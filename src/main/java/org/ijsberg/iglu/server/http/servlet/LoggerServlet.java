package org.ijsberg.iglu.server.http.servlet;

import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.logging.Logger;
import org.ijsberg.iglu.util.misc.StringSupport;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class LoggerServlet extends HttpServlet implements Logger {

    private List<LogEntry> logEntryQueue = new ArrayList();

    private static final int MAX = 100;

    /**
     * @param conf
     * @throws ServletException
     */
    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);
    }

    public void service(HttpServletRequest servletRequest, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");

        List<LogEntry> logEntries = new ArrayList<>(logEntryQueue);
        for(LogEntry logEntry : logEntries) {
            String entryString = logEntry.toString();
            entryString = StringSupport.replaceAll(entryString, "\n", "<br>");
            out.println("<span class=\"LogEntry\">" + entryString + "</span><br>");
        }
        out.println("<div id=\"end_of_log\"></div>");
    }

    @Override
    public void log(LogEntry entry) {
        if(logEntryQueue.size() > MAX) {
            logEntryQueue.remove(0);
        }
        logEntryQueue.add(entry);
    }

    @Override
    public void addAppender(Logger appender) {
    }

    @Override
    public void removeAppender(Logger appender) {
    }
}
