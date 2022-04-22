package com.soybeany.log.collector.statistic;

import com.soybeany.log.collector.common.data.BaseUnit;
import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.common.model.MsgRecorder;
import com.soybeany.log.collector.common.model.loader.LogLineHolder;
import com.soybeany.log.collector.common.service.BaseScanService;
import com.soybeany.log.collector.common.service.IUnitHandler;
import com.soybeany.log.core.model.LogPack;
import com.soybeany.util.cache.IDataHolder;
import com.soybeany.util.cache.StdMemDataHolder;

import java.io.File;
import java.io.IOException;

/**
 * @author Soybeany
 * @date 2022/4/22
 */
public class StatisticService<Statistic extends BaseUnit> extends BaseScanService<Statistic> {

    private final String unitDesc;
    private final MsgRecorder msgRecorder;
    private final IUnitHandler<Statistic> handler;

    private IDataHolder<Statistic> holder;

    public StatisticService(LogCollectConfig logCollectConfig, String unitDesc, MsgRecorder recorder, IUnitHandler<Statistic> handler) {
        super(logCollectConfig);
        this.unitDesc = unitDesc;
        this.msgRecorder = recorder;
        this.handler = handler;
        holder = new StdMemDataHolder<>(logCollectConfig.maxFileStatisticRetain);
    }

    @Override
    public Statistic onGetNewUnit(LogCollectConfig logCollectConfig, File logFile) {
        return handler.onGetNewUnit(logCollectConfig, logFile);
    }

    @Override
    public void onHandleLogLine(Statistic statistic, LogLineHolder holder) {
        handler.onHandleLogLine(statistic, holder);
    }

    @Override
    public void onHandleLogPack(Statistic statistic, LogPack logPack) {
        handler.onHandleLogPack(statistic, logPack);
    }

    public StatisticService<Statistic> statisticHolder(IDataHolder<Statistic> holder) {
        this.holder = holder;
        return this;
    }

    public Statistic updateAndGetUnit(File file) throws IOException {
        return updateAndGetUnit(unitDesc, msgRecorder, holder, file);
    }

}
