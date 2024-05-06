package com.lrs;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@MapperScan("com.lrs.mapper")
@SpringBootApplication
public class ExcelWordStartApp {
    public static void main(String[] args) {
        SpringApplication.run(ExcelWordStartApp.class, args);
    }
}
