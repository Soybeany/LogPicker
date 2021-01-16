package com.soybeany.log.collector.service.query;

import com.soybeany.log.collector.config.AppConfig;
import com.soybeany.log.collector.service.query.model.QueryContext;
import com.soybeany.log.collector.service.query.model.QueryParam;
import com.soybeany.log.core.model.LogException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Soybeany
 * @date 2021/1/7
 */
public interface QueryContextService {

    QueryContext initFromParam(Map<String, String> param);

    QueryContext createNewNextContextOf(QueryContext old);

}

@Service
class QueryContextServiceImpl implements QueryContextService {

    private static final String PREFIX = "context";

    private static final String P_KEY_ID = "id"; // 关联context的id，string

    @Autowired
    private AppConfig appConfig;

    private final Map<String, QueryContext> contextMap = new ConcurrentHashMap<>();

    @Override
    public QueryContext initFromParam(Map<String, String> param) {
        String contextId = QueryParam.getParam(PREFIX, P_KEY_ID, param);
        // 若指定了contextId，则尝试复用context
        QueryContext context;
        if (null != contextId) {
            if (!contextMap.containsKey(contextId)) {
                throw new LogException("找不到指定contextId对应的context");
            }
            context = contextMap.get(contextId);
        }
        // 创建新的context
        else {
            context = createNew(new QueryParam(appConfig, param));
        }
        return context;
    }

    @Override
    public QueryContext createNewNextContextOf(QueryContext old) {
        return createNew(old.queryParam);
    }

    private QueryContext createNew(QueryParam param) {
        QueryContext context = new QueryContext(param);
        contextMap.put(context.id, context);
        return context;
    }
}