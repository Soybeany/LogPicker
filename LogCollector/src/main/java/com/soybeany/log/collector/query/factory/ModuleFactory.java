package com.soybeany.log.collector.query.factory;

import com.soybeany.log.collector.query.data.QueryContext;
import com.soybeany.log.collector.query.processor.Preprocessor;

import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/24
 */
public interface ModuleFactory {

    /**
     * 设置预处理器
     */
    void onSetupPreprocessors(QueryContext context, List<Preprocessor> preprocessors);

}
