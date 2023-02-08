package com.soybeany.log.manager.executor;

import com.soybeany.exception.BdRtException;
import com.soybeany.log.core.model.QueryResultVO;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Soybeany
 * @date 2021/2/8
 */
public class HttpQueryExecutorImpl implements QueryExecutor {

    private static final OkHttpClient CLIENT_FOR_READ = new OkHttpClient.Builder().readTimeout(5, TimeUnit.MINUTES).build();

    @Override
    public QueryResultVO request(String url, Map<String, String> headers, Map<String, String[]> param) {
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }
        if (null == headers) {
            headers = Collections.emptyMap();
        }
        headers.put("content-type", "application/x-www-form-urlencoded;charset=utf-8");
        StringBuilder stringBuilder = new StringBuilder();
        param.forEach((name, values) -> {
            try {
                for (String value : values) {
                    stringBuilder.append(name).append("=").append(URLEncoder.encode(value, "utf-8")).append("&");
                }
            } catch (IOException e) {
                throw new RuntimeException("编码异常", e);
            }
        });
        if (stringBuilder.length() > 1) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        Request request = new Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded;charset=utf-8"), stringBuilder.toString()))
                .url(url)
                .headers(Headers.of(headers))
                .build();
        try (Response response = CLIENT_FOR_READ.newCall(request).execute()) {
            ResponseBody body;
            if (!response.isSuccessful() || null == (body = response.body())) {
                throw new IOException("“" + url + "”请求异常(" + response.code() + ")");
            }
            return GSON.fromJson(body.string(), QueryResultVO.class);
        } catch (IOException e) {
            throw new BdRtException(e.getMessage());
        }
    }

}
