package com.soybeany.log.manager;

import com.soybeany.log.core.model.LogPackForRead;
import com.soybeany.log.core.model.QueryResultVO;
import com.soybeany.log.manager.executor.QueryExecutor;

import java.util.*;

/**
 * @author Soybeany
 * @date 2021/2/9
 */
class CollectResult {
    private final Map<String, String> errMsgMap = new HashMap<>();
    private final Map<String, QueryResultVO> voMap = new HashMap<>();
    private final Comparator<LogPackForRead> comparator;

    public CollectResult(Comparator<LogPackForRead> comparator) {
        this.comparator = comparator;
    }

    public void add(Map<String, QueryExecutor.Dto<QueryResultVO>> dtoList) {
        dtoList.forEach((server, dto) -> {
            if (dto.isNorm) {
                voMap.put(server, dto.data);
            } else {
                errMsgMap.put(server, dto.msg);
            }
        });
    }

    public Set<String> getUidSet() {
        Set<String> set = new HashSet<>();
        for (QueryResultVO vo : voMap.values()) {
            vo.packs.forEach(pack -> set.add(pack.uid));
        }
        return set;
    }

    public List<Object> output(boolean needInfoMsg, Map<String, String> nextResultIdMap) {
        // 信息部分
        List<Map<String, Object>> infoPart = new LinkedList<>();
        for (String server : getAllServers()) {
            Map<String, Object> info = new LinkedHashMap<>();
            infoPart.add(info);
            info.put("server", server);
            // 设置正常信息
            setupNormInfo(server, info, needInfoMsg);
            // 设置异常信息
            setupErrMsg(server, info);
        }
        // vo处理
        List<LogPackForRead> logPart = new LinkedList<>();
        voMap.forEach((server, vo) -> {
            // 关联部分
            if (null != vo.info.nextResultId) {
                nextResultIdMap.put(server, vo.info.nextResultId);
            }
            // 日志部分
            vo.packs.forEach(pack -> {
                pack.server = server;
                logPart.add(pack);
            });
        });
        // 组装
        List<Object> output = new LinkedList<>();
        output.add(infoPart);
        output.addAll(getSorted(logPart));
        return output;
    }

    // ***********************内部方法****************************

    private List<LogPackForRead> getSorted(List<LogPackForRead> logPart) {
        Map<String, List<LogPackForRead>> tmp = new HashMap<>();
        // 分组
        for (LogPackForRead part : logPart) {
            tmp.computeIfAbsent(part.uid, k -> new ArrayList<>()).add(part);
        }
        // 组间排序
        List<List<LogPackForRead>> tmp2 = new ArrayList<>(tmp.values());
        tmp2.sort((g1, g2) -> comparator.compare(g1.get(0), g2.get(0)));
        // 组内排序并整合到最终结果
        List<LogPackForRead> result = new ArrayList<>();
        tmp2.forEach(list -> {
            list.sort(comparator);
            result.addAll(list);
        });
        return result;
    }

    private Set<String> getAllServers() {
        Set<String> servers = new HashSet<>();
        servers.addAll(errMsgMap.keySet());
        servers.addAll(voMap.keySet());
        return servers;
    }

    private void setupNormInfo(String server, Map<String, Object> info, boolean needInfoMsg) {
        QueryResultVO vo = voMap.get(server);
        if (null == vo) {
            return;
        }
        // 设置信息
        if (needInfoMsg) {
            info.put("msg", vo.info.msg);
        }
        // 设置结束原因
        info.put("endReason", vo.info.endReason);
    }

    private void setupErrMsg(String server, Map<String, Object> info) {
        String errMsg = errMsgMap.get(server);
        if (null == errMsg) {
            return;
        }
        info.put("errMsg", errMsg);
    }
}
