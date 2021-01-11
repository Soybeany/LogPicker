package com.soybeany.log.core.model;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/11
 */
public class StdLogVO {

    public Info info;

    public final List<StdLogItem> packs = new LinkedList<>();

    public static class Info {
        public String lastContextId;
        public String curContextId;
        public String nextContextId;
        public String endReason;
    }

}
