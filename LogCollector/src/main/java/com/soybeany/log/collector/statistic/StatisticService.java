package com.soybeany.log.collector.statistic;

import com.soybeany.log.collector.common.data.BaseUnit;
import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.common.model.MsgRecorder;
import com.soybeany.log.collector.common.model.loader.LogLineHolder;
import com.soybeany.log.collector.common.service.BaseScanService;
import com.soybeany.log.collector.common.service.IUnitHandler;
import com.soybeany.log.core.model.LogPack;

import java.io.File;
import java.io.IOException;

/**
 * @author Soybeany
 * @date 2022/4/22
 */
public class StatisticService<Unit extends BaseUnit> extends BaseScanService<Unit> {

    private final String unitDesc;
    private final MsgRecorder msgRecorder;
    private final IUnitHandler<Unit> handler;

    public StatisticService(LogCollectConfig logCollectConfig, String unitDesc, MsgRecorder recorder, IUnitHandler<Unit> handler) {
        super(logCollectConfig);
        this.unitDesc = unitDesc;
        this.msgRecorder = recorder;
        this.handler = handler;
    }

    @Override
    public Unit onGetNewUnit(LogCollectConfig logCollectConfig, File logFile) {
        return handler.onGetNewUnit(logCollectConfig, logFile);
    }

    @Override
    public void onHandleLogLine(Unit unit, LogLineHolder holder) {
        handler.onHandleLogLine(unit, holder);
    }

    @Override
    public void onHandleLogPack(Unit unit, LogPack logPack) {
        handler.onHandleLogPack(unit, logPack);
    }

    public Unit updateAndGetUnit(File file) throws IOException {
        return updateAndGetUnit(unitDesc, msgRecorder, null, file);
    }

}
