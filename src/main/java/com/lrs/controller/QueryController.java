package com.lrs.controller;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lrs.domian.ExcelWord;
import com.lrs.domian.ExcelWordData;
import com.lrs.service.ExcelWordsServices;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;


@CrossOrigin
@RestController
public class QueryController {
    private static final Logger log = LoggerFactory.getLogger(QueryController.class);
    @Autowired
    private ExcelWordsServices excelWordsServices;

    @RequestMapping(value = "/query/{keyWord}", method = RequestMethod.GET)
    public ResponseEntity<ExcelWordData> queryWord(@PathVariable String keyWord) {
        ExcelWordData excelWords = null;
        try {
            // 调API返回页面到本地
            String response = sendRequest(keyWord);
            // 处理response
            excelWords = getExcelWord(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new ResponseEntity<>(excelWords, HttpStatus.OK);
//        try {
//            excelWordsServices.saveExcelWordList(excelWords);
//        } catch (Exception e) {
//            System.err.println(e.toString());
//            throw new RuntimeException(e);
//        }
    }

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public ResponseEntity<String> save(@RequestBody List<ExcelWord> data) {
        try {
            ExcelWordData excelWordData = new ExcelWordData();
            excelWordData.setExcelWords(data);
            excelWordsServices.saveExcelWordList(excelWordData);
            return new ResponseEntity<>("保存成功", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/syncExcelToDB", method = RequestMethod.POST)
    public String syncExcelToDB(@RequestBody String data) {
        try {
            // 创建 ObjectMapper 对象
            ObjectMapper objectMapper = new ObjectMapper();
            // 解析 JSON 数组字符串并转换为 List<ExcelWord>
            List<ExcelWord> excelWordList = objectMapper.readValue(data, new TypeReference<List<ExcelWord>>() {});
            if (!Objects.isNull(excelWordList.get(0).getUser())) {
                JSONObject jsonObject = excelWordsServices.syncExcelToDB(excelWordList);
                // 遍历所有的键，检查是否对应的值为空，如果为空则移除该键值对
                jsonObject.keySet().removeIf(key -> jsonObject.getJSONArray(key).isEmpty());
                // 返回修改后的 JSONObject
                return !jsonObject.isEmpty() ? jsonObject.toJSONString() : "无变动";
            }
            return "用户未指定";
        } catch (Exception e) {
            // 处理异常
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * 收集数据
     *
     * @param response
     * @return
     */
    private ExcelWordData getExcelWord(String response) {
        ExcelWordData excelWordData = new ExcelWordData();
        List<ExcelWord> excelWordList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        Document doc = Jsoup.parse(response);
        // 判断是否多音
        Element ifMultiple = doc.selectFirst("header.word-details-header");
        if (ifMultiple == null) {
            ExcelWord excelWord = new ExcelWord();
            List<String> meanings = new ArrayList<>();
            // 查找翻译内容所在的元素
            Element translationElement = doc.selectFirst("div.word-text");

            if (translationElement != null) {
                // 提取翻译内容
                String translation = Objects.requireNonNull(translationElement.select("h2").first()).text();
                if (StringUtils.hasText(translation)) {
                    excelWord.setWord(translation);
                }
                System.out.println("翻译内容: " + translation);

                // 提取日文发音
                Element pronouncesElement = doc.selectFirst("div.pronounces");
                if (pronouncesElement != null) {
                    String japaneseKana = Objects.requireNonNull(pronouncesElement.select("span").first()).text();
                    if (StringUtils.hasText(japaneseKana)) {
                        // 去左右的 [ ]
                        japaneseKana = japaneseKana.replace("[", "").replace("]", "");
                        excelWord.setKana(japaneseKana);
                    }
                    System.out.println("日文发音: " + japaneseKana);
                } else {
                    System.out.println("未找到日文发音");
                }
            } else {
                System.out.println("未找到翻译内容");
            }

            // 提取中文注释
            // 查找class为simple的元素
            Element simpleElement = doc.selectFirst("div.simple");
            // 在 div.simple 元素下选择 <h2> 元素
            Element h2Element = simpleElement.selectFirst("h2");
            if (h2Element != null) {
                // 提取 <h2> 元素内容
                String h2Content = h2Element.text();
                if (!StringUtils.isEmpty(h2Content)) {
                    // 去 左右两边的【】
                    h2Content = h2Content.replace("【", "").replace("】", "");
                }
                excelWord.setType(h2Content);
                System.out.println(h2Content);
            } else {
                System.out.println("未找到type元素");
            }

            if (simpleElement != null) {
                // 查找所有的<li>元素并提取内容
                Elements liElements = simpleElement.select("li");
                for (Element liElement : liElements) {
                    // 获取li元素的文本内容，并去除首尾空格
                    String liContent = liElement.text().trim();
                    meanings.add(liContent);
                    System.out.println(liContent);
                }
                // 清空 StringBuilder 对象
                sb.setLength(0);
                for (String meaning : meanings) {
                    sb.append(meaning);
                }
                excelWord.setMeanings(sb.toString());
            } else {
                System.out.println("未找到class为simple的元素");
            }
            excelWordList.add(excelWord);

        } else {
            // 获取 <section class="word-details-content"> 元素
            Element sectionElement = doc.selectFirst("section.word-details-content");

            if (sectionElement != null) {
                // 循环遍历 <div class="word-details-pane"> 元素
                Elements wordDetailsPaneElements = sectionElement.select("div.word-details-pane");
                for (Element wordDetailsPane : wordDetailsPaneElements) {
                    ExcelWord excelWord = new ExcelWord();
                    List<String> meanings = new ArrayList<>();

                    // 提取 <div class="word-text"> 下的 <h2> 元素的文本内容
                    Element h2Element = wordDetailsPane.selectFirst("div.word-text h2");
                    String word = h2Element != null ? h2Element.text() : "";
                    excelWord.setWord(word);

                    // 提取 <div class="pronounces"> 下的 <span> 元素的文本内容
                    Element spanElement = wordDetailsPane.selectFirst("div.pronounces span");
                    String japaneseKana = spanElement != null ? spanElement.text() : "";
                    // 去左右的 [ ]
                    if (!StringUtils.isEmpty(japaneseKana)) {
                        japaneseKana = japaneseKana.replace("[", "").replace("]", "");
                        excelWord.setKana(japaneseKana);
                    }
                    // 提取 <div class="simple"> 下的 <h2> 元素
                    Element typeElement = wordDetailsPane.selectFirst("div.simple h2");
                    if (typeElement != null) {
                        // 提取 <h2> 元素内容
                        String h2Content = typeElement.text();
                        if (!StringUtils.isEmpty(h2Content)) {
                            // 去 左右两边的【】
                            h2Content = h2Content.replace("【", "").replace("】", "");
                            excelWord.setType(h2Content);
                        }
                        System.out.println(h2Content);
                    } else {
                        System.out.println("未找到type元素");
                    }

                    // 提取 <div class="simple"> 下的 <ul> 元素
                    Element ulElement = wordDetailsPane.selectFirst("div.simple ul");
                    if (ulElement != null) {
                        // 获取 <ul> 下所有的 <li> 元素
                        Elements liElements = ulElement.select("li");
                        for (Element li : liElements) {
                            // 处理 <li> 元素的文本内容
                            String meaning = li.text();
                            // 在这里可以进行进一步的处理或存储
                            meanings.add(meaning);
                        }
                        // 清空 StringBuilder 对象
                        sb.setLength(0);
                        for (String meaning : meanings) {
                            sb.append(meaning);
                        }
                        excelWord.setMeanings(sb.toString());
                    }
                    excelWordList.add(excelWord);
                }
            }
        }
        excelWordData.setExcelWords(excelWordList);
        return excelWordData;
    }

    /**
     * 调用API
     *
     * @param keyWord
     * @return
     */
    private String sendRequest(String keyWord) {
        try {
            // 对关键字进行 URL 编码
            String encodedKeyword = URLEncoder.encode(keyWord, "UTF-8");
            String url = "https://dict.hujiang.com/jp/jc/" + encodedKeyword;

            // 创建URL对象
            URL obj = new URL(url);
            // 打开连接
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // 设置请求方法
            con.setRequestMethod("GET");

            // 添加请求头(测试Cookie，不知道能用多久。失效再改)
            con.setRequestProperty("Cookie", "acw_tc=1a0c380817141972082353982e005bc3a406fe27e55ee346be4d41939abd25; HJ_UID=badc3397-fb39-bf71-b7ea-2da41ed345d1; HJ_CST=1; HJ_CSST_3=1; TRACKSITEMAP=3; _REF=; _SREF_3=; HJ_SID=g45mdv-f9be-4c62-bee8-5da802651a4a; HJ_SSID_3=g45mdv-d176-4694-a64e-715dd10206b2; _SREG_3=direct%7C%7Cdirect%7Cdirect; _REG=direct%7C%7Cdirect%7Cdirect");

            // 获取响应码
            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // 读取响应内容
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            // 对响应结果进行处理
            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}
