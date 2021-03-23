package com.soybeany.log.collector;

import com.soybeany.log.collector.common.RangeService;
import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.query.factory.KeyContainsModuleFactory;
import com.soybeany.log.collector.query.factory.ModuleFactory;
import com.soybeany.log.collector.query.factory.TagContainsModuleFactory;
import com.soybeany.log.collector.query.factory.UidModuleFactory;
import com.soybeany.log.collector.query.provider.FileProvider;
import com.soybeany.log.collector.query.service.QueryService;
import com.soybeany.log.core.util.DataHolder;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/2/10
 */
public class LogCollector {

    public static void init() {
        DataHolder.createTimer();
    }

    public static void release() {
        DataHolder.destroyTimer();
    }

    public static QueryBuilder query(LogCollectConfig logCollectConfig, FileProvider fileProvider) {
        return new QueryBuilder(logCollectConfig, fileProvider);
    }

    // ********************内部类********************

    public static class QueryBuilder {
        private final List<ModuleFactory> factories = new LinkedList<>();
        private final LogCollectConfig logCollectConfig;
        private final FileProvider fileProvider;

        public QueryBuilder(LogCollectConfig logCollectConfig, FileProvider fileProvider) {
            this.logCollectConfig = logCollectConfig;
            this.fileProvider = fileProvider;
            setupDefaultModuleFactories();
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

        public QueryService build() {
            return new QueryService(logCollectConfig, fileProvider, factories);
        }

        private void setupDefaultModuleFactories() {
            factories.add(new KeyContainsModuleFactory());
            factories.add(new TagContainsModuleFactory(logCollectConfig));
            factories.add(new UidModuleFactory(new RangeService(logCollectConfig)));
        }
    }

}
