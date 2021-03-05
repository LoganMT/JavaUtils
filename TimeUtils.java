package com.logan.demo.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class TimeUtils {

    public static LocalDateTime toDatetime(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }


    public static long toTimestamp(LocalDateTime ldt) {
        return ldt.toInstant(ZoneOffset.of("+8")).toEpochMilli();
    }

    public static String toTimestamp10(LocalDateTime ldt) {
        return String.valueOf(ldt.toInstant(ZoneOffset.of("+8")).toEpochMilli()).substring(0, 10);
    }

    public static long toTimestamp10L(LocalDateTime ldt) {
        return Long.parseLong(String.valueOf(ldt.toInstant(ZoneOffset.of("+8")).toEpochMilli()).substring(0, 10));
    }

    public static String format2yyyyMMdd(LocalDateTime ldt) {
        return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public static String format2yyyyMMddHHmmss(LocalDateTime ldt) {
        return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }


    public static LocalDate parseryyyyMMdd(String time) {
        DateTimeFormatter dfParse = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.parse(time, dfParse);
    }

    public static LocalDateTime parseryyyyMMddHHmmss(String time) {
        DateTimeFormatter dfParse = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(time, dfParse);
    }

}
