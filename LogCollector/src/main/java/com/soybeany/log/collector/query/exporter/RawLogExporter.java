package com.soybeany.log.collector.query.exporter;

import com.soybeany.log.collector.query.data.QueryResult;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.log.core.model.QueryRawResultVO;
import com.soybeany.util.HexUtils;
import com.soybeany.util.SerializeUtils;

import java.io.IOException;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/3/3
 */
public class RawLogExporter implements LogExporter<String> {

    @Override
    public String export(QueryResult result) {
        checkQueryResult(result);
        QueryRawResultVO vo = new QueryRawResultVO();
        LogExporter.setupResultInfo(result, vo.info);
        vo.packs.addAll(result.logPacks);
        try {
            return HexUtils.bytesToHex(SerializeUtils.serialize(vo));
        } catch (IOException e) {
            throw new LogException("结果导出异常:" + e.getMessage());
        }
    }

}
