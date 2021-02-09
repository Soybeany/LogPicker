package com.soybeany.log.core.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/1/11
 */
public class QueryRawResultVO implements Serializable {

    public final ResultInfo info = new ResultInfo();

    public final List<LogPack> packs = new LinkedList<>();

}
