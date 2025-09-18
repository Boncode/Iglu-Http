package org.ijsberg.iglu.server.http.module;

import org.eclipse.jetty.logging.JettyAppender;
import org.eclipse.jetty.logging.JettyLevel;
import org.eclipse.jetty.logging.JettyLogger;
import org.eclipse.jetty.logging.StdErrAppender;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.logging.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.impl.Slf4jHack;
import org.slf4j.impl.StaticLoggerBinder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.ijsberg.iglu.logging.Level.*;

public class IgluJettyLogAppender implements JettyAppender {
    @Override
    public void emit(JettyLogger jettyLogger, Level level, long l, String s, Throwable throwable, String s1, Object... objects) {
        System.out.println(new LogEntry(DEBUG, "[" + level + "] " + s1, objects));
    }

    public static void init(Logger logger) {
        try {
            StaticLoggerBinder binder;
            java.util.logging.Logger rootLogger = Slf4jHack.getJDK14Logger();
            rootLogger.addHandler(new IgluLoggerHandler());
            java.util.logging.Level javaLogLevel = IgluLoggerHandler.getMinimumJavaLogLevel(org.ijsberg.iglu.logging.Level.values()[logger.getLogLevelOrdinal()]);
            rootLogger.setLevel(javaLogLevel);
            System.out.println(new LogEntry(VERBOSE, "forwarding java log messages of minimum level " + javaLogLevel));
        } catch (Exception e) {
            try {
                System.out.println(new LogEntry(CRITICAL, "unable to initialize java log message forwarding", e));

//               StaticLoggerBinder binder;
                JettyLogger root = (JettyLogger) LoggerFactory.getLogger(JettyLogger.ROOT_LOGGER_NAME);

                IgluJettyLogAppender logAppender = new IgluJettyLogAppender();

                setFinalStatic(JettyLogger.class.getDeclaredField("appender"), root, logAppender);

//                root.addHandler(new IgluLoggerHandler());
                java.util.logging.Level javaLogLevel = IgluLoggerHandler.getMinimumJavaLogLevel(org.ijsberg.iglu.logging.Level.values()[logger.getLogLevelOrdinal()]);
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

}
