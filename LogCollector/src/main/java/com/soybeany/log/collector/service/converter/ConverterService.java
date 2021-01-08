package com.soybeany.log.collector.service.converter;

import com.soybeany.log.collector.model.QueryContext;

import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/8
 */
public interface ConverterService<From, To> {

    List<To> convert(QueryContext context, List<From> list);

}
