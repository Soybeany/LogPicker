package com.soybeany.log.core.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/11
 */
public class QueryResultVO implements Serializable {

    public final Info info = new Info();

    public final List<Object> packs = new LinkedList<>();

    public static class Info implements Serializable {
        public String lastContextId;
        public String curContextId;
        public String nextContextId;
        public List<String> msg;
        public String endReason;
    }

}
