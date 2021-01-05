package com.soybeany.log.collector.model;

import com.soybeany.log.collector.util.TimeUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class QueryParam {

    private static final String TAG_PREFIX = "tag-";

    public Date from = getDefaultFrom();
    public Date to = getDefaultTo();

    public final List<Kv> kvList = new LinkedList<>();

    public QueryParam(Map<String, String> param) {
        int tagLength = TAG_PREFIX.length();
        for (Map.Entry<String, String> entry : param.entrySet()) {
            if (!entry.getKey().startsWith(TAG_PREFIX)) {
                continue;
            }
            kvList.add(new Kv(entry.getKey().substring(tagLength), entry.getValue()));
        }
    }

    private Date getDefaultFrom() {
        LocalDateTime time = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).minusSeconds(1);
        return TimeUtils.toDate(time);
    }

    private Date getDefaultTo() {
        LocalDateTime time = LocalDateTime.now();
        return TimeUtils.toDate(time);
    }

    public static class Kv {
        public String key;
        public String value;

        public Kv(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
