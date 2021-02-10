package com.soybeany.log.collector.query;

import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.query.data.QueryResult;
import com.soybeany.log.core.model.Constants;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.util.DataTimingHolder;

import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
public class QueryResultService {

    private static final DataTimingHolder<QueryResult> RESULT_MAP = new DataTimingHolder<>();

    private final LogCollectConfig logCollectConfig;

    public QueryResultService(LogCollectConfig logCollectConfig) {
        this.logCollectConfig = logCollectConfig;
    }

    public QueryResult loadResultFromParam(Map<String, String> param) {
        String resultId = param.get(Constants.PARAM_RESULT_ID);
        if (null == resultId) {
            return null;
        }
        // 若指定了resultId，则尝试获取指定的result
        QueryResult result = RESULT_MAP.get(resultId);
        if (null == result) {
            throw new LogException("指定的resultId不存在或已过期");
        }
        return result;
    }

    public synchronized void registerResult(QueryResult result) {
        RESULT_MAP.set(result.id, result, logCollectConfig.resultRetainSec);
    }
}