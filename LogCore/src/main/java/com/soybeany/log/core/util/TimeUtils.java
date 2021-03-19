package com.soybeany.log.core.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * @author Soybeany
 * @date 2021/1/5
 */
public class TimeUtils {
    public TimeUtils() {
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static Date toDate(LocalDateTime time) {
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Long toMillis(LocalDateTime time) {
        return time.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
    }

    public static LocalDateTime fromMillis(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneOffset.ofHours(8)).toLocalDateTime();
    }
}
