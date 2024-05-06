package com.lrs.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lrs.domian.ExcelWord;
import com.lrs.domian.ExcelWordData;

import java.util.List;

public interface ExcelWordsServices extends IService<ExcelWord> {

    void saveExcelWordList(ExcelWordData data);

    JSONObject syncExcelToDB(List<ExcelWord> excelWords);
}
