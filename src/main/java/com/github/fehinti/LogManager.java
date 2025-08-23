package com.github.fehinti;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogManager {
    private LogManager() {}
    public static Logger getClassLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
