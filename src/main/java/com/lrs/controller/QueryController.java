package com.lrs.controller;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lrs.domian.ExcelWord;
import com.lrs.domian.ExcelWordData;
import com.lrs.service.ExcelWordsServices;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

@RestController
public class QueryController {
    @Autowired
    private ExcelWordsServices excelWordsServices;

    @RequestMapping("/query/{keyWord}")
    public void queryWord(@PathVariable String keyWord) {
        // 调API返回页面到本地
        String response = sendRequest(keyWord);
        // 处理response
        ExcelWordData excelWords = getExcelWord(response);
        try {
            excelWordsServices.saveExcelWordList(excelWords);
        } catch (Exception e) {
            System.err.println(e.toString());
            throw new RuntimeException(e);
        }
    }

    @RequestMapping(value = "/syncExcelToDB", method = RequestMethod.POST)
    public String syncExcelToDB(@RequestBody String data) {
        try {
            // 将接收到的 JSON 字符串解析为 Java 对象
            List<ExcelWord> excelWords = JSON.parseArray(data, ExcelWord.class);

            JSONObject jsonObject = excelWordsServices.syncExcelToDB(excelWords);

            // 遍历所有的键，检查是否对应的值为空，如果为空则移除该键值对
            for (Iterator<String> iterator = jsonObject.keySet().iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                if (jsonObject.getJSONArray(key).isEmpty()) {
                    iterator.remove();
                }
            }
            // 返回修改后的 JSONObject
            return jsonObject.size() > 0 ? jsonObject.toJSONString() : "无变动";
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
