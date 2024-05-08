package com.lrs.service.serviceImpl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lrs.domian.ExcelWord;
import com.lrs.domian.ExcelWordData;
import com.lrs.mapper.ExcelWordsMapper;
import com.lrs.service.ExcelWordsServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class ExcelWordsServicesImpl extends ServiceImpl<ExcelWordsMapper, ExcelWord> implements ExcelWordsServices {
    @Resource
    private ExcelWordsMapper excelWordsMapper;

    @Lazy
    @Autowired
    private ExcelWordsServices excelWordsServices;

    @Override
    public void saveExcelWordList(ExcelWordData data) {
        List<ExcelWord> excelWords = data.getExcelWords();
        for (ExcelWord excelWord : excelWords) {
            excelWord.setDate(LocalDate.now());

            // 先查
            QueryWrapper<ExcelWord> wrapper = new QueryWrapper<>();

            wrapper.eq("word", excelWord.getWord());
            wrapper.eq("kana", excelWord.getKana());
            wrapper.eq("type", excelWord.getType());
            wrapper.eq("date", excelWord.getDate());

            List<ExcelWord> excelWordList = excelWordsMapper.selectList(wrapper);

            if (excelWordList.size() > 0) {
                // 删除
                List<Long> ids = new ArrayList<>();
                excelWordList.forEach(word -> {
                    ids.add(word.getId());
                });
                excelWordsMapper.deleteBatchIds(ids);
            }

            // 插入
            excelWordsMapper.insert(excelWord);
        }

    }

    @Override
    public JSONObject syncExcelToDB(List<ExcelWord> excelWords) {
        JSONObject jsonObject = new JSONObject();

        for (ExcelWord excelWord : excelWords) {
            if (Objects.equals(excelWord.getWord(), "") || excelWord.getWord() == null) {
                return jsonObject;
            }
        }
        QueryWrapper<ExcelWord> wrapper = new QueryWrapper<>();
        wrapper.eq("date", excelWords.get(0).getDate());
        List<ExcelWord> excelWordList = excelWordsMapper.selectList(wrapper);
        // db数据
        System.out.println(excelWordList);
        // excel数据
        System.out.println(excelWords);

        List<ExcelWord> addExcelWords = new ArrayList<>();
        List<ExcelWord> delExcelWords = new ArrayList<>();
        List<ExcelWord> editExcelWords = new ArrayList<>();

        // 使用HashMap来存储数据库中的单词，以id为键
        Map<Long, ExcelWord> dbMap = excelWordList.stream()
                .collect(Collectors.toMap(ExcelWord::getId, Function.identity()));

        // 使用HashSet来存储需要删除的单词的id
        Set<Long> idsToDelete = new HashSet<>(dbMap.keySet());

        // 检查需要插入或更新的单词
        for (ExcelWord excelWord : excelWords) {
            ExcelWord dbWord = dbMap.get(excelWord.getId());
            if (dbWord != null) {
                // 如果id存在于数据库中，检查word和date是否需要更新
                if (!(excelWord.equals(dbWord))) {
                    editExcelWords.add(excelWord);
                }
                // 由于单词存在于Excel中，所以不应该删除
                idsToDelete.remove(excelWord.getId());
            } else {
                // 如果id不存在于数据库中，插入新的单词
                excelWord.setDate(LocalDate.now());
                addExcelWords.add(excelWord);
            }
        }

        // 删除不再出现在Excel列表中的单词
        for (Long id : idsToDelete) {
            ExcelWord excelWord = dbMap.get(id);
            delExcelWords.add(excelWord);
        }

        // 新增
        excelWordsServices.saveBatch(addExcelWords);
        // 修改
        for (ExcelWord editExcelWord : editExcelWords) {
            excelWordsMapper.update(editExcelWord,new QueryWrapper<ExcelWord>().eq("id",editExcelWord.getId()).eq("date",editExcelWord.getDate()));
        }
        // 删除
        excelWordsServices.removeBatchByIds(delExcelWords.stream().map(ExcelWord::getId).collect(Collectors.toList()));

        jsonObject.put("新增词汇", addExcelWords.stream().map(ExcelWord::getWord).collect(Collectors.toList()));
        jsonObject.put("删除词汇", delExcelWords.stream().map(ExcelWord::getWord).collect(Collectors.toList()));
        jsonObject.put("修改词汇", editExcelWords.stream().map(ExcelWord::getWord).collect(Collectors.toList()));
        return jsonObject;
    }
}
