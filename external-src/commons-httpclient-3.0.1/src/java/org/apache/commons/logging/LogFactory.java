package org.apache.commons.logging;

public class LogFactory {
    static final Log log = new Log();

    public static Log getLog(String name) {
        return log;
    }

    public static Log getLog(Class c) {
        return log;
    }
}