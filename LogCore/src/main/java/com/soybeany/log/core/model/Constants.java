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
    String TAG_BORDER_START = "BORDER_START";

    /**
     * 标签-边界结束
     */
    String TAG_BORDER_END = "BORDER_END";

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

}
