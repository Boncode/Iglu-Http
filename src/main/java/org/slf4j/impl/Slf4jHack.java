package org.slf4j.impl;

import org.eclipse.jetty.logging.JettyLogger;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.logging.Logger;
import org.slf4j.LoggerFactory;

import static org.ijsberg.iglu.logging.Level.CRITICAL;
import static org.ijsberg.iglu.logging.Level.VERBOSE;

public class Slf4jHack {
    public static void init(Logger logger) {
        try {
            StaticLoggerBinder binder;
            JDK14LoggerAdapter root = (JDK14LoggerAdapter) LoggerFactory.getLogger(JettyLogger.ROOT_LOGGER_NAME);
            root.logger.addHandler(new IgluLoggerHandler());
            java.util.logging.Level javaLogLevel = IgluLoggerHandler.getMinimumJavaLogLevel(Level.values()[logger.getLogLevelOrdinal()]);
            root.logger.setLevel(javaLogLevel);
            System.out.println(new LogEntry(VERBOSE, "forwarding java log messages of minimum level " + javaLogLevel));
        } catch (Exception e) {
            System.out.println(new LogEntry(CRITICAL, "unable to initialize log message forwarding", e));
        }
    }
}
