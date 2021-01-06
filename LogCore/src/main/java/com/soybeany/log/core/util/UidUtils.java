package com.soybeany.log.core.util;

import java.util.UUID;

/**
 * @author Soybeany
 * @date 2021/1/6
 */
public class UidUtils {

    public static String getNew() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

}
