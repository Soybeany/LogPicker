package com.soybeany.log.core.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Soybeany
 * @date 2021/2/10
 */
public class DataHolder<T> {

    private static ScheduledExecutorService SERVICE;

    private final Map<String, Task<T>> dataMap;

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

    public DataHolder(int maxCount) {
        dataMap = new LruMap<>(maxCount);
    }

    public synchronized void put(String key, T data, int expiryInSec) {
        String uid = scheduleTask(key, expiryInSec);
        dataMap.put(key, new Task<>(uid, data, expiryInSec));
    }

    public T updateAndGet(String key) {
        Task<T> task = dataMap.get(key);
        if (null == task) {
            return null;
        }
        task.uid = scheduleTask(key, task.expiryInSec);
        return task.data;
    }

    public synchronized void remove(String key) {
        dataMap.remove(key);
    }

    // ********************内部方法********************

    private String scheduleTask(String key, int expiryInSec) {
        String uid = UidUtils.getNew();
        SERVICE.schedule(() -> removeData(key, uid), expiryInSec, TimeUnit.SECONDS);
        return uid;
    }

    private synchronized void removeData(String key, String uid) {
        Task<T> task = dataMap.get(key);
        if (null == task || !uid.equals(task.uid)) {
            return;
        }
        dataMap.remove(key);
    }

    // ********************内部类********************

    private static class LruMap<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        public LruMap(int capacity) {
            super(0, 0.75f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    private static class Task<T> {
        String uid;
        T data;
        int expiryInSec;

        public Task(String uid, T data, int expiryInSec) {
            this.uid = uid;
            this.data = data;
            this.expiryInSec = expiryInSec;
        }
    }

}