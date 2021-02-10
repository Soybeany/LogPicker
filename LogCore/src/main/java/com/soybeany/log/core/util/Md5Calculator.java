package com.soybeany.log.core.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * @author Soybeany
 * @date 2021/2/10
 */
public class Md5Calculator {

    private String curMd5 = "";

    public Md5Calculator with(Object obj) throws Exception {
        if (null != obj) {
            String newMd5 = calculateMd5(obj.toString());
            curMd5 = calculateMd5(curMd5 + newMd5);
        }
        return this;
    }

    public String calculate() {
        return curMd5;
    }

    private String calculateMd5(String msg) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(msg.getBytes(StandardCharsets.UTF_8));
        byte[] s = digest.digest();
        StringBuilder result = new StringBuilder();
        for (byte b : s) {
            result.append(Integer.toHexString((0x000000FF & b) | 0xFFFFFF00).substring(6));
        }
        return result.toString();
    }

}
