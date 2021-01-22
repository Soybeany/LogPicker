package com.soybeany.log.collector.service.query;

import com.soybeany.log.collector.service.query.model.QueryContext;
import com.soybeany.log.collector.service.query.model.QueryParam;
import com.soybeany.log.core.model.LogException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
public interface QueryContextService {

    @Nullable
    QueryContext loadContextFromParam(Map<String, String> param);

    void registerContext(QueryContext context);

}

@Service
class QueryContextServiceImpl implements QueryContextService {

    private static final String PREFIX = "context";

    private static final String P_KEY_ID = "id"; // 关联context的id，string

    private final Map<String, QueryContext> contextMap = new WeakHashMap<>();

    @Override
    public QueryContext loadContextFromParam(Map<String, String> param) {
        String contextId = QueryParam.getParam(PREFIX, P_KEY_ID, param);
        if (null == contextId) {
            return null;
        }
        // 若指定了contextId，则尝试获取指定的context
        if (!contextMap.containsKey(contextId)) {
            throw new LogException("找不到指定contextId对应的context");
        }
        return contextMap.get(contextId);
    }

    @Override
    public synchronized void registerContext(QueryContext context) {
        contextMap.put(context.id, context);
    }
}