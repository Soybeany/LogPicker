package com.soybeany.log.collector.query.data;

import com.soybeany.log.core.util.UidUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/2/2
 */
public class QueryResult {

    public final String id = UidUtils.getNew();
    public final QueryContext context;
    public final Map<File, QueryIndexes> indexesMap;

    public String lastId;
    public String nextId;

    /**
     * 结果的内容
     */
    public Object content;

    public String endReason = "已完成搜索";

    public final long createTime = System.currentTimeMillis();
    public long finishTime;

    /**
     * 查询过程中的一些信息
     */
    public final List<String> msgList = new LinkedList<>();

    public QueryResult(QueryContext context, Map<File, QueryIndexes> indexesMap) {
        this.context = context;
        this.indexesMap = indexesMap;
    }

    // ********************公开方法********************

    public void lock() {
        context.lock.lock();
    }

    public void unlock() {
        context.lock.unlock();
    }

    public void setFinished() {
        finishTime = System.currentTimeMillis();
        msgList.add("查询耗时:" + (finishTime - createTime) + "ms");
    }

    public List<String> getAllMsg() {
        List<String> totalMsg = new LinkedList<>();
        totalMsg.addAll(context.msgList);
        totalMsg.addAll(msgList);
        return totalMsg;
    }
}
