package com.lrs.domian;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@TableName("excel_word")
public class ExcelWord implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 汉字
     */
    @TableField("word")
    private String word;
    /**
     * 假名(发音)
     */
    @TableField("kana")
    private String kana;
    /**
     * 词性
     */
    @TableField("type")
    private String type;
    /**
     * 释义
     */
    @TableField(value = "meanings")
    private String meanings;

    /**
     * 录入日期
     */
    @TableField("date")
    @JsonFormat(pattern = "yyyy/M/d")
    private LocalDate date;
}
