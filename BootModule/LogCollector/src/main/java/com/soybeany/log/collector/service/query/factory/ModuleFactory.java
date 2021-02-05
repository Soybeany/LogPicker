package com.soybeany.log.collector.service.query.factory;

import com.soybeany.log.collector.service.query.data.QueryContext;
import com.soybeany.log.collector.service.query.processor.Preprocessor;
import org.springframework.lang.NonNull;

import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/24
 */
public interface ModuleFactory {

    /**
     * 设置预处理器
     */
    @NonNull
    void onSetupPreprocessors(QueryContext context, List<Preprocessor> preprocessors);

}
