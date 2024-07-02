package com.soybeany.log.collector.common.service;

import com.soybeany.config.BDCipherUtils;
import com.soybeany.log.collector.common.data.BaseUnit;
import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.common.model.MsgRecorder;
import com.soybeany.log.collector.common.model.loader.ILogLineLoader;
import com.soybeany.log.collector.common.model.loader.LogPackLoader;
import com.soybeany.log.collector.common.model.loader.SimpleLogLineLoader;
import com.soybeany.log.core.model.LogException;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.util.cache.IDataHolder;

import java.io.File;
import java.io.IOException;

/**
 * @author Soybeany
 * @date 2022/4/22
 */
public abstract class BaseScanService<Unit extends BaseUnit> implements IUnitHandler<Unit> {

    protected final LogCollectConfig logCollectConfig;

    protected BaseScanService(LogCollectConfig logCollectConfig) {
        this.logCollectConfig = logCollectConfig;
    }

    protected Unit updateAndGetUnit(String unitDesc, MsgRecorder recorder, IDataHolder<Unit> unitHolder, File file) throws IOException {
        // 得到索引
        Unit unit = getUnit(unitDesc, recorder, unitHolder, file);
        try {
            unit.lock.lock();
            long startByte = unit.scannedBytes;
            // 若索引已是最新，则不再更新
            if (file.length() == startByte) {
                recorder.write("“" + file.getName() + "”的" + unitDesc + "不需更新(" + startByte + ")");
                return unit;
            }
            // 更新索引
            long startTime = System.currentTimeMillis();
            try (SimpleLogLineLoader lineLoader = new SimpleLogLineLoader(unit.logFile, logCollectConfig.logCharset, logCollectConfig.lineParsePattern, logCollectConfig.tagParsePattern, logCollectConfig.lineTimeFormatter);
                 LogPackLoader<ILogLineLoader> packLoader = new LogPackLoader<>(lineLoader, logCollectConfig.noUidPlaceholder, logCollectConfig.maxLinesPerResultWithNoUid, unit.uidTempMap)) {
                lineLoader.resetTo(startByte, null); // 因为不会有旧数据，理论上这里不会null异常
                packLoader.setListener(holder -> onHandleLogLine(unit, holder));
                LogPack logPack;
                while (null != (logPack = packLoader.loadNextCompleteLogPack())) {
                    onHandleLogPack(unit, logPack);
                }
                unit.scannedBytes = lineLoader.getReadPointer();
            }
            long spendTime = System.currentTimeMillis() - startTime;
            recorder.write("“" + file.getName() + "”的" + unitDesc + "已更新(" + startByte + "~" + unit.scannedBytes + ")，耗时" + spendTime + "ms");
            return unit;
        } finally {
            unit.lock.unlock();
        }
    }

    // ***********************内部方法****************************

    private Unit getUnit(String unitDesc, MsgRecorder recorder, IDataHolder<Unit> unitHolder, File file) {
        String unitKey = getUnitKey(file);
        Unit unit = null != unitHolder ? unitHolder.get(unitKey) : null;
        try {
            if (null != unit) {
                unit.check(logCollectConfig);
                return unit;
            }
        } catch (LogException e) {
            recorder.write("重新创建“" + file.getName() + "”的" + unitDesc + "(" + e.getMessage() + ")");
        }
        unit = onGetNewUnit(logCollectConfig, file);
        if (null != unitHolder) {
            unitHolder.put(unitKey, unit, logCollectConfig.indexRetainSec);
        }
        return unit;
    }

    private String getUnitKey(File logFile) {
        try {
            return BDCipherUtils.calculateMd5(logFile.getAbsolutePath());
        } catch (Exception e) {
            throw new LogException(e);
        }
    }

}
