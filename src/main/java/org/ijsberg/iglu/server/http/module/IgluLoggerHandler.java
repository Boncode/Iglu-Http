package org.ijsberg.iglu.server.http.module;

import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static java.util.logging.Level.*;

public class IgluLoggerHandler extends Handler {

    @Override
    public void publish(LogRecord record) {
        System.out.println(new LogEntry(getIgluLogLevel(record.getLevel()), "[" + record.getSourceClassName()
                + "." + record.getSourceMethodName() + "] " + record.getMessage(), record.getThrown()));
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }

    /*
    Java util levels:

    1000 : SEVERE
    900 : WARNING
    800 : INFO
    700 : CONFIG
    500 : FINE
    400 : FINER
     */

    static Level getIgluLogLevel(java.util.logging.Level javaLogLevel) {
        if(javaLogLevel.intValue() > 900) {
            return Level.CRITICAL;
        }
        if(javaLogLevel.intValue() > 700) {
            return Level.VERBOSE;
        }
        if(javaLogLevel.intValue() > 500) {
            return Level.DEBUG;
        }
        return Level.TRACE;
    }

    static java.util.logging.Level getMinimumJavaLogLevel(Level igluLogLevel) {
        switch (igluLogLevel) {
            case CRITICAL: return WARNING;
            case VERBOSE: return INFO;
            case DEBUG: return FINE;
            //case TRACE: return ALL;
            default: return ALL;
        }
    }
}
