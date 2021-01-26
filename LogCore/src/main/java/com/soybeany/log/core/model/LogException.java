package com.soybeany.log.core.model;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class LogException extends RuntimeException {
    public LogException(String msg) {
        super(msg);
    }

    public LogException(Exception e) {
        super(e);
    }
}
