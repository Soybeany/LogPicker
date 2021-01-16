package com.soybeany.log.collector.service.common.parser;

import com.soybeany.log.collector.service.common.model.ILogReceiver;

import java.util.regex.Pattern;

/**
 * @author Soybeany
 * @date 2021/1/12
 */
public interface LogParser {

    /**
     * 批量解析前的回调
     */
    default void beforeBatchParse() {
    }

    /**
     * 批量解析后的回调
     */
    default void afterBatchParse(ILogReceiver receiver) {
    }

    /**
     * 解析单行时的回调
     */
    void onParse(Pattern pattern, long fromByte, long toByte, String lineString, ILogReceiver receiver);

}
