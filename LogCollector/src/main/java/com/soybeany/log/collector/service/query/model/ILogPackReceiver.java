package com.soybeany.log.collector.service.query.model;

import com.soybeany.log.core.model.LogPack;

/**
 * @author Soybeany
 * @date 2021/1/20
 */
public interface ILogPackReceiver {

    default void onStart() {
    }

    default void onFinish(long bytesRead, long actualEndPointer) {
    }

    /**
     * @return 是否继续遍历
     */
    boolean onReceive(LogPack logPack);

}
