package com.soybeany.log.collector;

import com.soybeany.log.collector.common.RangeService;
import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.query.QueryService;
import com.soybeany.log.collector.query.exporter.LogExporter;
import com.soybeany.log.collector.query.exporter.StdLogExporter;
import com.soybeany.log.collector.query.factory.KeyContainsModuleFactory;
import com.soybeany.log.collector.query.factory.ModuleFactory;
import com.soybeany.log.collector.query.factory.TagContainsModuleFactory;
import com.soybeany.log.collector.query.factory.UidModuleFactory;
import com.soybeany.log.collector.scan.ScanService;
import com.soybeany.log.core.util.DataTimingHolder;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/2/10
 */
public class LogCollector {

    public static void init() {
        DataTimingHolder.createTimer();
    }

    public static void release() {
        DataTimingHolder.destroyTimer();
    }

    public static ScanBuilder scan(LogCollectConfig logCollectConfig) {
        return new ScanBuilder(logCollectConfig);
    }

    public static QueryBuilder query(LogCollectConfig logCollectConfig) {
        return new QueryBuilder(logCollectConfig);
    }

    // ********************内部类********************

    private static abstract class BaseBuilder<T> {
        public abstract T build();
    }

    public static class ScanBuilder extends BaseBuilder<ScanService> {
        private final LogCollectConfig logCollectConfig;

        public ScanBuilder(LogCollectConfig logCollectConfig) {
            this.logCollectConfig = logCollectConfig;
        }

        @Override
        public ScanService build() {
            return new ScanService(logCollectConfig);
        }
    }

    public static class QueryBuilder extends BaseBuilder<QueryService> {
        private final LogCollectConfig logCollectConfig;
        private final List<ModuleFactory> factories = new LinkedList<>();
        private LogExporter logExporter;

        public QueryBuilder(LogCollectConfig logCollectConfig) {
            this.logCollectConfig = logCollectConfig;
            setupDefaultModuleFactories();
            setupDefaultExporter();
        }

        @Override
        public QueryService build() {
            return new QueryService(logCollectConfig, factories, logExporter);
        }

        public QueryBuilder moduleFactories(List<ModuleFactory> factories) {
            this.factories.clear();
            this.factories.addAll(factories);
            return this;
        }

        public QueryBuilder addModuleFactory(ModuleFactory factory) {
            factories.add(factory);
            return this;
        }

        public QueryBuilder logExporter(LogExporter logExporter) {
            this.logExporter = logExporter;
            return this;
        }

        private void setupDefaultModuleFactories() {
            factories.add(new KeyContainsModuleFactory());
            factories.add(new TagContainsModuleFactory(logCollectConfig));
            factories.add(new UidModuleFactory(new RangeService(logCollectConfig)));
        }

        private void setupDefaultExporter() {
            logExporter = new StdLogExporter(logCollectConfig);
        }
    }

}
