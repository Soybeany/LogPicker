package com.soybeany.log.collector.service.query.data;

import com.soybeany.log.core.util.UidUtils;

/**
 * @author Soybeany
 * @date 2021/2/2
 */
public class QueryResult {

    public final String id = UidUtils.getNew();
    public final QueryContext context;

    public String lastId;
    public String nextId;

    /**
     * 结果的文本
     */
    public String text;

    public String endReason = "已搜索全部日志";

    public QueryResult(QueryContext context) {
        this.context = context;
    }

    public QueryResult(QueryParam queryParam) {
        this.context = new QueryContext(queryParam);
    }

    // ********************公开方法********************

    public void lock() {
        context.lock.lock();
    }

    public void unlock() {
        context.lock.unlock();
    }
}
