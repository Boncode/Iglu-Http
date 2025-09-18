package org.slf4j.impl;

import org.eclipse.jetty.logging.JettyLogger;
import org.slf4j.LoggerFactory;

public class Slf4jHack {

    public static java.util.logging.Logger getJDK14Logger() {
        return ((JDK14LoggerAdapter)LoggerFactory.getLogger(JettyLogger.ROOT_LOGGER_NAME)).logger;
    }
}
