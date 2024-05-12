package com.lrs.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class GlobalDateConfig {
    // 序列化时设置的时间格式
    private final String formatter = "yyyy/MM/dd HH:mm:ss";
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            DateTimeFormatter localDateTimeFormatter = DateTimeFormatter.ofPattern(formatter);
            //返回时间数据序列化
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(localDateTimeFormatter));
            // 接收时间数据反序列化
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(localDateTimeFormatter));
        };
    }
}

