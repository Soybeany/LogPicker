package com.soybeany.log.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Soybeany
 * @date 2021/2/10
 */
public class DataTimingHolder<T> {

    private static ScheduledExecutorService SERVICE;

    private final Map<String, Task<T>> dataMap = new HashMap<>();

    @SuppressWarnings("AlibabaThreadPoolCreation")
    public synchronized static void createTimer() {
        if (null == SERVICE) {
            SERVICE = Executors.newScheduledThreadPool(1);
        }
    }

    public synchronized static void destroyTimer() {
        if (null != SERVICE) {
            SERVICE.shutdown();
            SERVICE = null;
        }
    }

    public synchronized void set(String key, T data, int expiryInSec) {
        String uid = UidUtils.getNew();
        dataMap.put(key, new Task<>(uid, data));
        SERVICE.schedule(() -> {
            Task<T> task = dataMap.get(key);
            if (null == task || !uid.equals(task.uid)) {
                return;
            }
            dataMap.remove(key);
        }, expiryInSec, TimeUnit.SECONDS);
    }

    public T get(String key) {
        Task<T> task = dataMap.get(key);
        if (null == task) {
            return null;
        }
        return task.data;
    }

    private static class Task<T> {
        String uid;
        T data;

        public Task(String uid, T data) {
            this.uid = uid;
            this.data = data;
        }
    }

}
