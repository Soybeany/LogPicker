package com.soybeany.log.manager.executor;

import com.google.gson.Gson;
import com.soybeany.log.core.model.QueryResultVO;

import java.util.Map;

/**
 * @author Soybeany
 * @date 2023/2/8
 */
public interface QueryExecutor {

    Gson GSON = new Gson();

    QueryResultVO request(String url, Map<String, String> headers, Map<String, String[]> param);

    class Dto<T> {
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
