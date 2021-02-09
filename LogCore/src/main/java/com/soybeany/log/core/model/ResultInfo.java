package com.soybeany.log.core.model;

import java.io.Serializable;
import java.util.List;

/**
 * @author Soybeany
 * @date 2021/2/8
 */
public class ResultInfo extends IdOwner implements Serializable {
    public List<String> msg;
    public String endReason;
}
