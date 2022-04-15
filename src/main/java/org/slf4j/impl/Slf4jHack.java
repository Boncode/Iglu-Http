package org.slf4j.impl;

import org.eclipse.jetty.logging.JettyLevel;
import org.eclipse.jetty.logging.JettyLogger;
import org.eclipse.jetty.logging.StdErrAppender;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.logging.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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
            try {
                System.out.println(new LogEntry(CRITICAL, "unable to initialize java log message forwarding", e));

//               StaticLoggerBinder binder;
                JettyLogger root = (JettyLogger) LoggerFactory.getLogger(JettyLogger.ROOT_LOGGER_NAME);

                IgluJettyLogAppender logAppender = new IgluJettyLogAppender();

                setFinalStatic(JettyLogger.class.getDeclaredField("appender"), root, logAppender);

//                root.addHandler(new IgluLoggerHandler());
                java.util.logging.Level javaLogLevel = IgluLoggerHandler.getMinimumJavaLogLevel(Level.values()[logger.getLogLevelOrdinal()]);
                root.setLevel(JettyLevel.ALL);
//                root.setLevel(JettyLevel.valueOf(javaLogLevel.getName()));
                //System.out.println(root.getLevel());
//                System.out.println(new LogEntry(VERBOSE, "forwarding java log messages of minimum level " + javaLogLevel));
                System.out.println(new LogEntry(VERBOSE, "APPENDER: logging messages of minimum level " + root.getLevel() + " to standard out"));

                ((StdErrAppender)root.getAppender()).setStream(System.out);

            } catch (Exception e2) {
                System.out.println(new LogEntry(CRITICAL, "unable to initialize jetty log message forwarding", e2));
            }
        }
    }


    static void setFinalStatic(Field field, Object object, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(object, newValue);
    }
    public static void main(String args[]) throws Exception {
       //setFinalStatic(Boolean.class.getField("FALSE"), true);

        //System.out.format("Everything is %s", false); // "Everything is true"
    }
}
