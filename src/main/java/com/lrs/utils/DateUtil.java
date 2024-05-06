package com.lrs.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    // 日期格式化
    public static String now() {
        // 获取当前日期
        LocalDate currentDate = LocalDate.now();
        // 指定日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 格式化日期
        return currentDate.format(formatter);
    }
}
