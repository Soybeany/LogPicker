package com.soybeany.log.collector;

import com.soybeany.log.collector.common.data.LogCollectConfig;
import com.soybeany.log.collector.common.data.LogIndexes;
import com.soybeany.log.collector.common.service.RangeService;
import com.soybeany.log.collector.query.data.QueryResult;
import com.soybeany.log.collector.query.factory.KeyContainsModuleFactory;
import com.soybeany.log.collector.query.factory.ModuleFactory;
import com.soybeany.log.collector.query.factory.TagContainsModuleFactory;
import com.soybeany.log.collector.query.factory.UidModuleFactory;
import com.soybeany.log.collector.query.provider.FileProvider;
import com.soybeany.log.collector.query.service.QueryService;
import com.soybeany.log.core.model.IDataHolder;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/2/10
 */
public class LogCollector {

    public static QueryBuilder query(LogCollectConfig logCollectConfig) {
        return new QueryBuilder(logCollectConfig);
    }

    // ********************内部类********************

    public static class QueryBuilder {
        private final List<ModuleFactory> factories = new LinkedList<>();
        private final LogCollectConfig logCollectConfig;

        public QueryBuilder(LogCollectConfig logCollectConfig) {
            this.logCollectConfig = logCollectConfig;
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

        public QueryService build(FileProvider fileProvider, IDataHolder<LogIndexes> indexesHolder, IDataHolder<QueryResult> resultHolder) {
            return new QueryService(logCollectConfig, fileProvider, factories, indexesHolder, resultHolder);
        }

        private void setupDefaultModuleFactories() {
            factories.add(new KeyContainsModuleFactory());
            factories.add(new TagContainsModuleFactory(logCollectConfig));
            factories.add(new UidModuleFactory(new RangeService(logCollectConfig)));
        }
    }

}
