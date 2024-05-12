package com.lrs;

import com.lrs.domian.ExcelWord;
import com.lrs.mapper.ExcelWordsMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class mapperTest {
    @Autowired
    ExcelWordsMapper excelWordsMapper;

    @Test
    public void test01(){
        List<ExcelWord> excelWordList = excelWordsMapper.selectList(null);
        System.out.println(excelWordList);
    }



}
