package com.soybeany.log.collector.query.data;

import com.soybeany.log.collector.common.data.LogIndexes;
import com.soybeany.log.collector.query.processor.LogFilter;
import com.soybeany.log.core.model.FileRange;
import com.soybeany.log.core.model.LogPack;

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
     * 已经返回过的uid记录
     */
    public final Set<String> returnedUidSet = new HashSet<>();
    /**
     * 使用uid-thread作为key，存放查询范围时，未组装完成的临时记录
     */
    public final Map<String, LogPack> uidTempMap = new HashMap<>();

    /**
     * 查询过程中的一些信息（包括续查均显示）
     */
    public final List<String> msgList = new LinkedList<>();

    public QueryContext(QueryParam queryParam) {
        this.queryParam = queryParam;
    }

    // ********************公开方法********************

    public String getParam(String prefix, String key) {
        return queryParam.getParams(prefix).get(key);
    }

}
