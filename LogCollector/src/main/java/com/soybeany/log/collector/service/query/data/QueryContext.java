package com.soybeany.log.collector.service.query.data;

import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.collector.service.query.model.LogFilter;
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

    /**
     * 请求中指定的查询参数
     */
    public final QueryParam queryParam;
    /**
     * 使用到的索引
     */
    public final Map<File, LogIndexes> indexesMap;
    /**
     * 未经过滤的uid记录
     */
    public final Set<String> unfilteredUidSet;
    /**
     * 待查询的范围
     */
    public final Map<File, List<FileRange>> queryRanges;
    /**
     * 使用的日志过滤器
     */
    public final List<LogFilter> filters;

    /**
     * 过滤后未使用的结果
     */
    public final LinkedList<LogPack> unusedFilteredResults;
    /**
     * 已经使用过(已返回)的uid记录
     */
    public final Set<String> usedUidSet;
    /**
     * 使用uid-thread作为key，存放查询范围时，未组装完成的临时记录
     */
    public final Map<String, LogPack> uidTempMap;

    public final Map<String, Object> data;
    public final Map<String, Object> tempData = new HashMap<>();

    public String lastId;
    public String nextId;
    public String endReason = "已搜索全部日志";
    public String result;

    @SuppressWarnings("CopyConstructorMissesField")
    public QueryContext(QueryContext context) {
        this.queryParam = context.queryParam;
        this.indexesMap = context.indexesMap;
        this.unfilteredUidSet = context.unfilteredUidSet;
        this.queryRanges = context.queryRanges;
        this.filters = context.filters;
        this.unusedFilteredResults = context.unusedFilteredResults;
        this.usedUidSet = context.usedUidSet;
        this.uidTempMap = context.uidTempMap;
        this.data = context.data;
    }

    public QueryContext(QueryParam queryParam) {
        this.queryParam = queryParam;
        this.indexesMap = new LinkedHashMap<>();
        this.unfilteredUidSet = new LinkedHashSet<>();
        this.queryRanges = new LinkedHashMap<>();
        this.filters = new LinkedList<>();
        this.unusedFilteredResults = new LinkedList<>();
        this.usedUidSet = new HashSet<>();
        this.uidTempMap = new HashMap<>();
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
