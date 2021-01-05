package com.soybeany.log.collector.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class Context {
    public final QueryParam param;
    public final Map<String, String> data = new HashMap<>();

    public Context(QueryParam param) {
        this.param = param;
    }
}
