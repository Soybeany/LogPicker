package com.soybeany.log.manager;

import com.soybeany.log.core.model.*;
import com.soybeany.log.core.util.UidUtils;
import com.soybeany.log.manager.executor.QueryExecutor;
import com.soybeany.util.cache.IDataHolder;
import com.soybeany.util.cache.StdMemDataHolder;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author Soybeany
 * @date 2021/2/5
 */
public class QueryManager {

    private static final String KEY_LOG_SEARCH_HOSTS = "logSearchHosts";
    private static final String KEY_UID_SEARCH_HOSTS = "uidSearchHosts";
    private static final String KEY_HIDE_MSG = "hideMsg";
    private static final String KEY_FROM_TIME = "fromTime";
    private static final String KEY_TO_TIME = "toTime";
    private static final String KEY_UID_LIST = "uidList";
    private static final String HOST_SEPARATE_REGEX = "[,;]";

    private final QueryExecutor queryExecutor;
    private final IDataHolder<ResultHolder> holderMap;
    private UrlProvider logSearchUrlProvider;
    private UrlProvider uidSearchUrlProvider;

    public QueryManager(QueryExecutor queryExecutor, UrlProvider defaultUrlProvider, int maxResultCount) {
        this.queryExecutor = queryExecutor;
        holderMap = new StdMemDataHolder<>(maxResultCount, true);
        logSearchUrlProvider = uidSearchUrlProvider = defaultUrlProvider;
    }

    public QueryManager logSearchUrlProvider(UrlProvider provider) {
        logSearchUrlProvider = provider;
        return this;
    }

    public QueryManager uidSearchUrlProvider(UrlProvider provider) {
        uidSearchUrlProvider = provider;
        return this;
    }

    public String getResult(Map<String, String> headers, Map<String, String[]> param, int expiryInSec) {
        return getResult(headers, param, expiryInSec, Comparator.comparing(o -> o.time));
    }

    public String getResult(Map<String, String> headers, Map<String, String[]> param, int expiryInSec, Comparator<LogPackForRead> comparator) {
        String resultId = getSingleValueFromMap(param, Constants.PARAM_RESULT_ID);
        ResultHolder holder;
        Map<String, String> nextResultIdMap = new HashMap<>();
        if (null != resultId) {
            holder = holderMap.get(resultId);
            if (null == holder) {
                throw new LogException("找不到指定resultId对应的result");
            }
            // 若已有现成的结果，则直接返回
            if (null != holder.result) {
                return holder.getResultString();
            }
            // 使用resultId进行查找
            holder.result = getNewResultsByResultId(holder.uidSearchHosts, holder.resultIdMap, holder.headers, holder.param, nextResultIdMap, comparator);
        } else {
            // 使用参数进行查找
            Set<String> logSearchHosts = toHostSet(param.remove(KEY_LOG_SEARCH_HOSTS));
            Set<String> uidSearchHosts = toHostSet(param.remove(KEY_UID_SEARCH_HOSTS));
            checkHosts(logSearchHosts, uidSearchHosts);
            List<Object> list = getNewResultsByParam(logSearchHosts, uidSearchHosts, headers, param, nextResultIdMap, comparator);
            holder = getNewHolder(headers, param, list, uidSearchHosts, expiryInSec);
        }
        // 按需分页
        if (!nextResultIdMap.isEmpty()) {
            generateNextHolder(holder, nextResultIdMap);
        }
        // 加入id信息
        holder.result.add(0, holder.idOwner);
        return holder.getResultString();
    }

    // ********************内部方法********************

    private List<Object> getNewResultsByParam(Set<String> logSearchHosts, Set<String> uidSearchHosts, Map<String, String> headers, Map<String, String[]> param, Map<String, String> nextResultIdMap, Comparator<LogPackForRead> comparator) {
        CollectResult result = new CollectResult(comparator);
        // 获取第一批结果(根据查询条件)
        Set<String> urls = logSearchHosts.stream().map(host -> logSearchUrlProvider.onGetUrl(host)).collect(Collectors.toSet());
        Map<String, QueryExecutor.Dto<QueryResultVO>> firstDtoMap = batchInvoke(urls, url -> getResultByParam(url, headers, param));
        result.add(firstDtoMap);
        // 获取第二批结果(按需，根据uid)
        return getSecondPartResultsByUid(headers, param, result, uidSearchHosts, nextResultIdMap);
    }

    private List<Object> getNewResultsByResultId(Set<String> uidSearchHosts, Map<String, String> resultIdMap, Map<String, String> headers, Map<String, String[]> param, Map<String, String> nextResultIdMap, Comparator<LogPackForRead> comparator) {
        CollectResult result = new CollectResult(comparator);
        // 获取第一批结果(根据查询条件)
        Map<String, QueryExecutor.Dto<QueryResultVO>> firstDtoMap = batchInvoke(resultIdMap.keySet(), url -> getResultByResultId(url, headers, param, resultIdMap.get(url)));
        result.add(firstDtoMap);
        // 获取第二批结果(按需，根据uid)
        return getSecondPartResultsByUid(headers, param, result, uidSearchHosts, nextResultIdMap);
    }

    private List<Object> getSecondPartResultsByUid(Map<String, String> headers, Map<String, String[]> param, CollectResult result, Set<String> uidSearchHosts, Map<String, String> nextResultIdMap) {
        Set<String> uidSet;
        if (null != uidSearchHosts && !(uidSet = result.getUidSet()).isEmpty()) {
            Set<String> urls = uidSearchHosts.stream().map(host -> uidSearchUrlProvider.onGetUrl(host)).collect(Collectors.toSet());
            Map<String, QueryExecutor.Dto<QueryResultVO>> secondDtoMap = batchInvoke(urls, url -> getResultByUid(url, headers, param, uidSet));
            result.add(secondDtoMap);
        }
        // 转换为最终结果
        return result.output(!Boolean.parseBoolean(getSingleValueFromMap(param, KEY_HIDE_MSG)), nextResultIdMap);
    }

    private void checkHosts(Set<String> logSearchHosts, Set<String> uidSearchHosts) {
        if (null == logSearchHosts) {
            throw new LogException("未使用“logSearchHosts”指定要进行日志搜索的服务器");
        }
        if (null == uidSearchHosts) {
            return;
        }
        Set<String> temp = new HashSet<>(logSearchHosts);
        temp.retainAll(uidSearchHosts);
        if (!temp.isEmpty()) {
            throw new LogException("uidSearchHosts中不能含有logSearchHosts中指定的服务器");
        }
    }

    private Set<String> toHostSet(String[] hostsArr) {
        if (null == hostsArr || hostsArr.length == 0) {
            return null;
        }
        String hosts = hostsArr[0];
        if (null == hosts) {
            return null;
        }
        return new HashSet<>(Arrays.asList(hosts.split(HOST_SEPARATE_REGEX)));
    }

    private Map<String, QueryExecutor.Dto<QueryResultVO>> batchInvoke(Set<String> urls, BatchInvokeCallback callback) {
        Map<String, Callable<QueryResultVO>> callables = new HashMap<>();
        for (String url : urls) {
            callables.put(url, () -> callback.onInvoke(url));
        }
        return invokeAll(callables);
    }

    @SuppressWarnings("AlibabaThreadPoolCreation")
    private <T> Map<String, QueryExecutor.Dto<T>> invokeAll(Map<String, Callable<T>> tasks) {
        ExecutorService service = Executors.newCachedThreadPool();
        try {
            Map<String, Future<T>> futures = new HashMap<>();
            tasks.forEach((k, v) -> futures.put(k, service.submit(v)));
            Map<String, QueryExecutor.Dto<T>> result = new HashMap<>();
            futures.forEach((k, v) -> {
                try {
                    result.put(k, new QueryExecutor.Dto<>(true, v.get(), null));
                } catch (InterruptedException | ExecutionException e) {
                    result.put(k, new QueryExecutor.Dto<>(false, null, e.getMessage()));
                }
            });
            return result;
        } finally {
            service.shutdownNow();
        }
    }

    private QueryResultVO getResultByResultId(String url, Map<String, String> headers, Map<String, String[]> param, String resultId) {
        Map<String, String[]> newParam = new HashMap<>(param);
        setSingleValueToMap(newParam, Constants.PARAM_RESULT_ID, resultId);
        return getResultByParam(url, headers, newParam);
    }

    private QueryResultVO getResultByUid(String url, Map<String, String> headers, Map<String, String[]> param, Set<String> uidSet) {
        Iterator<String> it = uidSet.iterator();
        StringBuilder uidListBuilder = new StringBuilder(it.next());
        while (it.hasNext()) {
            uidListBuilder.append(";").append(it.next());
        }
        Map<String, String[]> newParam = new HashMap<>();
        newParam.put(KEY_FROM_TIME, param.get(KEY_FROM_TIME));
        newParam.put(KEY_TO_TIME, param.get(KEY_TO_TIME));
        setSingleValueToMap(newParam, KEY_UID_LIST, uidListBuilder.toString());
        return getResultByParam(url, headers, newParam);
    }

    private QueryResultVO getResultByParam(String url, Map<String, String> headers, Map<String, String[]> param) {
        return queryExecutor.request(url, headers, param);
    }

    private ResultHolder getNewHolder(Map<String, String> headers, Map<String, String[]> param, List<Object> result, Set<String> uidSearchHosts, int expiryInSec) {
        return new ResultHolder(headers, param, null, result, uidSearchHosts, expiryInSec);
    }

    private void generateNextHolder(ResultHolder last, Map<String, String> resultIdMap) {
        ResultHolder next = new ResultHolder(last.headers, last.param, resultIdMap, null, last.uidSearchHosts, last.expiryInSec);
        last.idOwner.nextResultId = next.idOwner.curResultId;
        next.idOwner.lastResultId = last.idOwner.curResultId;
    }

    private String getSingleValueFromMap(Map<String, String[]> map, String key) {
        return Optional.ofNullable(map.get(key))
                .map(v -> v.length > 0 ? v[0] : null)
                .orElse(null);
    }

    private void setSingleValueToMap(Map<String, String[]> map, String key, String value) {
        map.put(key, new String[]{value});
    }

    // ********************内部类********************

    private interface BatchInvokeCallback {
        QueryResultVO onInvoke(String url) throws IOException;
    }

    private class ResultHolder {
        public final IdOwner idOwner = new IdOwner();
        public final Map<String, String> headers;
        public final Map<String, String[]> param;
        public final Map<String, String> resultIdMap;
        public final Set<String> uidSearchHosts;
        private final int expiryInSec;
        public List<Object> result;

        private ResultHolder(Map<String, String> headers, Map<String, String[]> param, Map<String, String> resultIdMap, List<Object> result, Set<String> uidSearchHosts, int expiryInSec) {
            this.headers = headers;
            this.param = param;
            this.resultIdMap = resultIdMap;
            this.result = result;
            this.uidSearchHosts = uidSearchHosts;
            String uid = UidUtils.getNew();
            idOwner.curResultId = uid;
            holderMap.put(uid, this, this.expiryInSec = expiryInSec);
        }

        public String getResultString() {
            return QueryExecutor.GSON.toJson(result);
        }
    }

}
