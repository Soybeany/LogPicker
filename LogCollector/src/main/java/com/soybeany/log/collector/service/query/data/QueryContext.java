package com.soybeany.log.collector.service.query.data;

import com.soybeany.log.collector.service.common.data.LogIndexes;
import com.soybeany.log.collector.service.query.processor.LogFilter;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogPack;
import org.springframework.lang.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class QueryContext {

    public final Lock lock = new ReentrantLock();

    /**
     * 请求中指定的查询参数
     */
    public final QueryParam queryParam;
    /**
     * 使用到的索引
     */
    public final Map<File, LogIndexes> indexesMap = new LinkedHashMap<>();
    /**
     * 未经过滤的uid记录
     */
    public final Set<String> unfilteredUidSet = new LinkedHashSet<>();
    /**
     * 待查询的范围
     */
    public final Map<File, List<FileRange>> queryRanges = new LinkedHashMap<>();
    /**
     * 使用的日志过滤器
     */
    public final List<LogFilter> filters = new LinkedList<>();

    /**
     * 过滤后未使用的结果
     */
    public final LinkedList<LogPack> unusedFilteredResults = new LinkedList<>();
    /**
     * 已经使用过(已返回)的uid记录
     */
    public final Set<String> usedUidSet = new HashSet<>();
    /**
     * 使用uid-thread作为key，存放查询范围时，未组装完成的临时记录
     */
    public final Map<String, LogPack> uidTempMap = new HashMap<>();

    /**
     * 查询过程中的一些信息
     */
    public final List<String> msgList = new LinkedList<>();

    public QueryContext(QueryParam queryParam) {
        this.queryParam = queryParam;
    }

    // ********************公开方法********************

    @Nullable
    public String getParam(String prefix, String key) {
        return queryParam.getParams(prefix).get(key);
    }

    public long getQueryRangeBytes() {
        long bytes = 0;
        for (List<FileRange> ranges : queryRanges.values()) {
            for (FileRange range : ranges) {
                bytes += (range.to - range.from);
            }
        }
        return bytes;
    }

    // ********************内部方法********************

}
