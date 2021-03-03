package com.soybeany.log.collector.query.exporter;

import com.google.gson.Gson;
import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.query.data.QueryResult;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.log.core.model.QueryRawResultVO;
import com.soybeany.log.core.model.QueryResultVO;
import com.soybeany.util.HexUtils;
import com.soybeany.util.SerializeUtils;

import java.io.IOException;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/3/3
 */
public class RawLogExporter extends BaseLogExporter {

    public RawLogExporter(LogCollectConfig logCollectConfig) {
        super(logCollectConfig);
    }

    @Override
    public String export(QueryResult result, List<LogPack> packs) {
        QueryRawResultVO vo = new QueryRawResultVO();
        setupResultInfo(result, vo.info);
        vo.packs.addAll(packs);
        try {
            return HexUtils.bytesToHex(SerializeUtils.serialize(vo));
        } catch (IOException e) {
            throw new LogException("结果导出异常:" + e.getMessage());
        }
    }

    @Override
    protected String toString(Gson gson, QueryResultVO vo) {
        return null;
    }
}
