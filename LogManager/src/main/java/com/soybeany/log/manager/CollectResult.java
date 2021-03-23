package com.soybeany.log.manager;

import com.soybeany.log.core.model.LogPackForRead;
import com.soybeany.log.core.model.QueryResultVO;

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

    public void add(Map<String, BaseExecutor.Dto<QueryResultVO>> dtoList) {
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
        // 排序
        logPart.sort(comparator);
        // 组装
        List<Object> output = new LinkedList<>();
        output.add(infoPart);
        output.addAll(logPart);
        return output;
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
