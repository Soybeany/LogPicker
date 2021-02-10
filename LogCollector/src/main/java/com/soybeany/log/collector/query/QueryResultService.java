package com.soybeany.log.collector.query;

import com.soybeany.log.collector.query.data.QueryResult;
import com.soybeany.log.core.model.Constants;
import com.soybeany.log.core.model.LogException;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
public class QueryResultService {

    // todo 修改为定时器自动删除
    private final Map<String, QueryResult> resultMap = new WeakHashMap<>();

    public QueryResult loadResultFromParam(Map<String, String> param) {
        String resultId = param.get(Constants.PARAM_RESULT_ID);
        if (null == resultId) {
            return null;
        }
        // 若指定了resultId，则尝试获取指定的result
        if (!resultMap.containsKey(resultId)) {
            throw new LogException("找不到指定resultId对应的result");
        }
        return resultMap.get(resultId);
    }

    public synchronized void registerResult(QueryResult result) {
        resultMap.put(result.id, result);
    }
}