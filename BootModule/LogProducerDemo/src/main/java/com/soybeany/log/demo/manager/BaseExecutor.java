package com.soybeany.log.demo.manager;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Soybeany
 * @date 2021/2/8
 */
public class BaseExecutor {

    private static final String PATH_SEPARATOR = "/";
    private static final OkHttpClient CLIENT_FOR_READ = new OkHttpClient.Builder().readTimeout(5, TimeUnit.MINUTES).build();
    private static final Gson GSON = new Gson();
    @SuppressWarnings("AlibabaThreadPoolCreation")
    private static ExecutorService SERVICE;

    public synchronized static void createRequestPool() {
        if (null == SERVICE) {
            SERVICE = Executors.newCachedThreadPool();
        }
    }

    public synchronized static void destroyRequestPool() {
        if (null != SERVICE) {
            SERVICE.shutdown();
            SERVICE = null;
        }
    }

    protected static <T> Map<String, Dto<T>> invokeAll(Map<String, Callable<T>> tasks) {
        Map<String, Future<T>> futures = new HashMap<>();
        tasks.forEach((k, v) -> futures.put(k, SERVICE.submit(v)));
        Map<String, Dto<T>> result = new HashMap<>();
        futures.forEach((k, v) -> {
            try {
                result.put(k, new Dto<>(true, v.get(), null));
            } catch (InterruptedException | ExecutionException e) {
                result.put(k, new Dto<>(false, null, e.getMessage()));
            }
        });
        return result;
    }

    protected <T> T getService(String host, Class<T> service) {
        if (!host.startsWith("http")) {
            host = "http://" + host;
        }
        if (!host.endsWith(PATH_SEPARATOR)) {
            host = host + PATH_SEPARATOR;
        }
        Retrofit retrofit = new Retrofit.Builder()
                .client(CLIENT_FOR_READ)
                .baseUrl(host)
                .addConverterFactory(GsonConverterFactory.create(GSON))
                .build();
        return retrofit.create(service);
    }

    protected <T> T getBody(Response<T> response) throws IOException {
        if (!response.isSuccessful()) {
            throw new IOException("“" + response.raw().request().url() + "”请求异常(" + response.code() + ")");
        }
        return response.body();
    }

    // ********************内部类********************

    public static class Dto<T> {
        public boolean isNorm;
        public T data;
        public String msg;

        public Dto(boolean isNorm, T data, String msg) {
            this.isNorm = isNorm;
            this.data = data;
            this.msg = msg;
        }
    }

}
