package com.soybeany.log.collector.model;

import com.soybeany.log.core.model.ResultVO;
import com.soybeany.log.core.util.UidUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class QueryContext {
    private static final String SEPARATOR = "-";

    public final String id = UidUtils.getNew();
    public final Lock lock = new ReentrantLock();
    public final QueryParam queryParam;
    public final Map<String, Object> data = new HashMap<>();
    public final Map<String, Object> tempData = new HashMap<>();
    public ResultVO result;

    public QueryContext(QueryParam queryParam) {
        this.queryParam = queryParam;
    }

    public void putData(String prefix, String key, Object value) {
        data.put(getRealKey(prefix, key), value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(String prefix, String key) {
        return (T) data.get(getRealKey(prefix, key));
    }

    public void putTempData(String prefix, String key, Object value) {
        tempData.put(getRealKey(prefix, key), value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getTempData(String prefix, String key) {
        return (T) tempData.get(getRealKey(prefix, key));
    }

    public void clearTempData() {
        tempData.clear();
    }

    private String getRealKey(String prefix, String key) {
        if (null == prefix) {
            return key;
        }
        return prefix + SEPARATOR + key;
    }

    public interface NextContextHandler {
        void onHandleNextContext(QueryContext old, QueryContext next);
    }
}
