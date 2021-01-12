package com.soybeany.log.core.model;

/**
 * @author Soybeany
 * @date 2021/1/8
 */
public interface Constants {

    // ********************TAG********************

    /**
     * 标签-边界开始
     */
    String TAG_BORDER_START = "border_start";

    /**
     * 标签-边界结束
     */
    String TAG_BORDER_END = "border_end";

    // ********************EXPORT********************

    /**
     * 使用序列化导出(可更高级地加工)
     */
    String EXPORT_IN_SERIALIZE = "inSerialize";

    /**
     * 导出供阅读(可二次加工)
     */
    String EXPORT_FOR_READ = "forRead";

    /**
     * 导出供阅读(不再加工)
     */
    String EXPORT_FOR_DIRECT_READ = "forDirectRead";

    // ********************PARSER_KEY********************

    String PARSER_KEY_TIME = "time";
    String PARSER_KEY_UID = "uid";
    String PARSER_KEY_THREAD = "thread";
    String PARSER_KEY_LEVEL = "level";
    String PARSER_KEY_CONTENT = "content";
    String PARSER_KEY_KEY = "key";
    String PARSER_KEY_VALUE = "value";

}
