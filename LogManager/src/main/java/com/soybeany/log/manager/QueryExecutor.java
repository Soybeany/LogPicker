package com.soybeany.log.manager;

import com.soybeany.log.core.model.Constants;
import com.soybeany.log.core.model.IdOwner;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.QueryResultVO;
import com.soybeany.log.core.util.UidUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * @author Soybeany
 * @date 2021/2/5
 */
public class QueryExecutor extends BaseExecutor {

    private static final String KEY_LOG_SEARCH_HOSTS = "logSearchHosts";
    private static final String KEY_UID_SEARCH_HOSTS = "uidSearchHosts";
    private static final String KEY_HIDE_MSG = "hideMsg";
    private static final String HOST_SEPARATE_REGEX = "[,;]";

    private static final Map<String, ResultHolder> HOLDER_MAP = new WeakHashMap<>();

    private static synchronized void saveResultId(String uid, ResultHolder holder) {
        HOLDER_MAP.put(uid, holder);
    }

    public String getResult(Map<String, String> param) {
        String resultId = param.get(Constants.PARAM_RESULT_ID);
        ResultHolder holder;
        Map<String, String> nextResultIdMap = new HashMap<>();
        if (null != resultId) {
            holder = HOLDER_MAP.get(resultId);
            if (null == holder) {
                throw new LogException("找不到指定resultId对应的result");
            }
            // 若已有现成的结果，则直接返回
            if (null != holder.result) {
                return holder.getResultString();
            }
            // 使用resultId进行查找
            holder.result = getNewResultsByResultId(holder.uidSearchHosts, holder.resultIdMap, holder.param, nextResultIdMap);
        } else {
            // 使用参数进行查找
            Set<String> logSearchHosts = toHostSet(param.remove(KEY_LOG_SEARCH_HOSTS));
            Set<String> uidSearchHosts = toHostSet(param.remove(KEY_UID_SEARCH_HOSTS));
            checkHosts(logSearchHosts, uidSearchHosts);
            List<Object> list = getNewResultsByParam(logSearchHosts, uidSearchHosts, param, nextResultIdMap);
            holder = ResultHolder.getNew(param, list, uidSearchHosts);
        }
        // 按需分页
        if (!nextResultIdMap.isEmpty()) {
            ResultHolder.generateNext(holder, nextResultIdMap);
        }
        // 加入id信息
        holder.result.add(0, holder.idOwner);
        return holder.getResultString();
    }

    // ********************内部方法********************

    private List<Object> getNewResultsByParam(Set<String> logSearchHosts, Set<String> uidSearchHosts, Map<String, String> param, Map<String, String> nextResultIdMap) {
        CollectResult result = new CollectResult();
        // 获取第一批结果(根据查询条件)
        Map<String, Dto<QueryResultVO>> firstDtoMap = batchInvoke(logSearchHosts, host -> getResultByParam(host, param));
        result.add(firstDtoMap);
        // 获取第二批结果(按需，根据uid)
        return getSecondPartResultsByUid(param, result, uidSearchHosts, nextResultIdMap);
    }

    private List<Object> getNewResultsByResultId(Set<String> uidSearchHosts, Map<String, String> resultIdMap, Map<String, String> param, Map<String, String> nextResultIdMap) {
        CollectResult result = new CollectResult();
        // 获取第一批结果(根据查询条件)
        Map<String, Dto<QueryResultVO>> firstDtoMap = batchInvoke(resultIdMap.keySet(), host -> getResultByResultId(host, param, resultIdMap.get(host)));
        result.add(firstDtoMap);
        // 获取第二批结果(按需，根据uid)
        return getSecondPartResultsByUid(param, result, uidSearchHosts, nextResultIdMap);
    }

    private List<Object> getSecondPartResultsByUid(Map<String, String> param, CollectResult result, Set<String> uidSearchHosts, Map<String, String> nextResultIdMap) {
        Set<String> uidSet;
        if (null != uidSearchHosts && !(uidSet = result.getUidSet()).isEmpty()) {
            Map<String, Dto<QueryResultVO>> secondDtoMap = batchInvoke(uidSearchHosts, host -> getResultByUid(host, param, uidSet));
            result.add(secondDtoMap);
        }
        // 转换为最终结果
        return result.output(!Boolean.parseBoolean(param.get(KEY_HIDE_MSG)), nextResultIdMap);
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
            throw new LogException("searchUidHosts中不能含有searchLogHosts中指定的服务器");
        }
    }

    private Set<String> toHostSet(String hosts) {
        if (null == hosts) {
            return null;
        }
        return new HashSet<>(Arrays.asList(hosts.split(HOST_SEPARATE_REGEX)));
    }

    private Map<String, Dto<QueryResultVO>> batchInvoke(Set<String> hosts, BatchInvokeCallback callback) {
        Map<String, Callable<QueryResultVO>> callables = new HashMap<>();
        for (String host : hosts) {
            callables.put(host, () -> callback.onInvoke(host));
        }
        return invokeAll(callables);
    }

    private QueryResultVO getResultByResultId(String host, Map<String, String> param, String resultId) throws IOException {
        Map<String, String> newParam = new HashMap<>(param);
        newParam.put(Constants.PARAM_RESULT_ID, resultId);
        return getResultByParam(host, newParam);
    }

    private QueryResultVO getResultByUid(String host, Map<String, String> param, Set<String> uidSet) throws IOException {
        Map<String, String> newParam = new HashMap<>(param);
        Iterator<String> it = uidSet.iterator();
        StringBuilder uidListBuilder = new StringBuilder(it.next());
        while (it.hasNext()) {
            uidListBuilder.append(";").append(it.next());
        }
        newParam.put("uidList", uidListBuilder.toString());
        return getResultByParam(host, newParam);
    }

    private QueryResultVO getResultByParam(String host, Map<String, String> param) throws IOException {
        return request(host, "/query/forPack", param, QueryResultVO.class);
    }

    // ********************内部类********************

    private interface BatchInvokeCallback {
        QueryResultVO onInvoke(String host) throws IOException;
    }

    private static class ResultHolder {
        public final IdOwner idOwner = new IdOwner();
        public final Map<String, String> param;
        public final Map<String, String> resultIdMap;
        public final Set<String> uidSearchHosts;
        public List<Object> result;

        public static ResultHolder getNew(Map<String, String> param, List<Object> result, Set<String> uidSearchHosts) {
            return new ResultHolder(param, null, result, uidSearchHosts);
        }

        public static void generateNext(ResultHolder last, Map<String, String> resultIdMap) {
            ResultHolder next = new ResultHolder(last.param, resultIdMap, null, last.uidSearchHosts);
            last.idOwner.nextResultId = next.idOwner.curResultId;
            next.idOwner.lastResultId = last.idOwner.curResultId;
        }

        private ResultHolder(Map<String, String> param, Map<String, String> resultIdMap, List<Object> result, Set<String> uidSearchHosts) {
            this.param = param;
            this.resultIdMap = resultIdMap;
            this.result = result;
            this.uidSearchHosts = uidSearchHosts;
            String uid = UidUtils.getNew();
            idOwner.curResultId = uid;
            saveResultId(uid, this);
        }

        public String getResultString() {
            return GSON.toJson(result);
        }
    }

}
