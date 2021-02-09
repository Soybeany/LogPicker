package com.soybeany.log.demo.manager;

import com.soybeany.log.core.model.QueryResultVO;
import retrofit2.Call;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

import java.util.Map;

/**
 * @author Soybeany
 * @date 2021/2/7
 */
public interface QueryService {

    @FormUrlEncoded
    @POST("query/forPack")
    Call<QueryResultVO> forPack(Map<String, String> param);

}
