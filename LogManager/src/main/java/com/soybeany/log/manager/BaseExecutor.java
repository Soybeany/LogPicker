package com.soybeany.log.manager;

import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Soybeany
 * @date 2021/2/8
 */
public class BaseExecutor {

    private static final OkHttpClient CLIENT_FOR_READ = new OkHttpClient.Builder().readTimeout(5, TimeUnit.MINUTES).build();
    protected static final Gson GSON = new Gson();

    @SuppressWarnings("AlibabaThreadPoolCreation")
    protected static <T> Map<String, Dto<T>> invokeAll(Map<String, Callable<T>> tasks) {
        ExecutorService service = Executors.newCachedThreadPool();
        try {
            Map<String, Future<T>> futures = new HashMap<>();
            tasks.forEach((k, v) -> futures.put(k, service.submit(v)));
            Map<String, Dto<T>> result = new HashMap<>();
            futures.forEach((k, v) -> {
                try {
                    result.put(k, new Dto<>(true, v.get(), null));
                } catch (InterruptedException | ExecutionException e) {
                    result.put(k, new Dto<>(false, null, e.getMessage()));
                }
            });
            return result;
        } finally {
            service.shutdownNow();
        }
    }

    protected <T> T request(String url, Map<String, String> headers, Map<String, String[]> param, Type type) throws IOException {
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }
        if (null == headers) {
            headers = Collections.emptyMap();
        }
        headers.put("content-type", "application/x-www-form-urlencoded;charset=utf-8");
        StringBuilder stringBuilder = new StringBuilder();
        param.forEach((name, values) -> {
            for (String value : values) {
                stringBuilder.append(name).append("=").append(value).append("&");
            }
        });
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        Request request = new Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), stringBuilder.toString()))
                .url(url)
                .headers(Headers.of(headers))
                .build();
        try (Response response = CLIENT_FOR_READ.newCall(request).execute()) {
            ResponseBody body;
            if (!response.isSuccessful() || null == (body = response.body())) {
                throw new IOException("“" + url + "”请求异常(" + response.code() + ")");
            }
            return GSON.fromJson(body.string(), type);
        }
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
