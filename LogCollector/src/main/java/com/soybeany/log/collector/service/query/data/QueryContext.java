package com.soybeany.log.collector.service.query.data;

import com.soybeany.log.collector.service.common.model.LogFilter;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.log.core.util.UidUtils;
import org.springframework.lang.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class QueryContext {
    private static final String SEPARATOR = "-";

    public final String id = UidUtils.getNew();
    public final Lock lock = new ReentrantLock();

    public final QueryParam queryParam;
    /**
     * 第一个key为uid，第二个key为文件，value为文件范围列表
     */
    public final Map<String, Map<File, List<FileRange>>> uidMap;
    /**
     * 使用uid作为key，存放未组装完成的临时记录
     */
    public final Map<String, LogPack> uidTempMap;
    /**
     * 使用的日志过滤器
     */
    public final List<LogFilter> filters;

    public final Map<String, Object> data;
    public final Map<String, Object> tempData = new HashMap<>();

    public String lastId;
    public String nextId;
    public String endReason = "已搜索全部日志";
    public String result;

    @SuppressWarnings("CopyConstructorMissesField")
    public QueryContext(QueryContext context) {
        this.queryParam = context.queryParam;
        this.uidMap = context.uidMap;
        this.uidTempMap = context.uidTempMap;
        this.filters = context.filters;
        this.data = context.data;
    }

    public QueryContext(QueryParam queryParam) {
        this.queryParam = queryParam;
        this.uidMap = new LinkedHashMap<>();
        this.uidTempMap = new HashMap<>();
        this.filters = new LinkedList<>();
        this.data = new HashMap<>();
    }

    // ********************公开方法********************

    public void putData(String prefix, String key, Object value) {
        data.put(getRealKey(prefix, key), value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(String prefix, String key) {
        return (T) data.get(getRealKey(prefix, key));
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(String prefix, String key, Function<String, Object> function) {
        return (T) data.computeIfAbsent(getRealKey(prefix, key), function);
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

    @Nullable
    public String getParam(String prefix, String key) {
        return queryParam.getParams(prefix).get(key);
    }

    // ********************内部方法********************

    private String getRealKey(String prefix, String key) {
        if (null == prefix) {
            return key;
        }
        return prefix + SEPARATOR + key;
    }

}
