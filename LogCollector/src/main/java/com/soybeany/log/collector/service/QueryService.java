package com.soybeany.log.collector.service;

import com.soybeany.log.collector.model.QueryContext;
import com.soybeany.log.core.model.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public interface QueryService {

    @NonNull
    ResultVO query(Map<String, String> param);

}

@Service
class QueryServiceImpl implements QueryService {

    @Autowired
    private QueryContextService queryContextService;
    @Autowired
    private QueryResultService queryResultService;
    @Autowired
    private List<QueryContext.IListener> queryContextListeners;

    @Override
    public ResultVO query(Map<String, String> param) {
        QueryContext context = loadAndInitTempData(param);
        try {
            // 获取锁
            context.lock.lock();
            // 如果context中已包含结果，则直接返回
            if (null != context.result) {
                return context.result;
            }
            return context.result = queryResultService.getResult(context);
        } finally {
            context.clearTempData();
            // 释放锁
            context.lock.unlock();
        }
    }

    // ********************内部方法********************

    private QueryContext loadAndInitTempData(Map<String, String> param) {
        QueryContext context = queryContextService.loadContext(param);
        // 回调监听器
        for (QueryContext.IListener listener : queryContextListeners) {
            listener.onInitTempData(context);
        }
        return context;
    }

}
