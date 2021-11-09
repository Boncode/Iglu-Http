package org.slf4j.impl;

import org.eclipse.jetty.logging.JettyAppender;
import org.eclipse.jetty.logging.JettyLogger;
import org.ijsberg.iglu.logging.LogEntry;
import org.slf4j.event.Level;

import static org.ijsberg.iglu.logging.Level.DEBUG;
import static org.slf4j.impl.IgluLoggerHandler.getIgluLogLevel;

public class IgluJettyLogAppender implements JettyAppender {
    @Override
    public void emit(JettyLogger jettyLogger, Level level, long l, String s, Throwable throwable, String s1, Object... objects) {
        System.out.println(new LogEntry(DEBUG, "[" + level + "] " + s1, objects));
    }
}
