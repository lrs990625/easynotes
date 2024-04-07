package com.lrs.controller;


import com.lrs.domian.ExcelWord;
import com.lrs.domian.ExcelWordResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Controller
public class QueryController {
    @RequestMapping("/query/{keyWord}")
    public void queryWord(@PathVariable String keyWord) {
        // 调API返回页面到本地
        String response = sendRequest(keyWord);

        // 处理response
        ExcelWordResponse excelWords = getExcelWord(response);

        // 输出到excel
        writeToExcel(excelWords);
    }


    // 日期格式化
    public String dateFormat() {
        // 获取当前日期
        LocalDate currentDate = LocalDate.now();

        // 指定日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 格式化日期
        String formattedDate = currentDate.format(formatter);

        return formattedDate;
    }

    /**
     * 输出到excel
     */
    private void writeToExcel(ExcelWordResponse excelWords) {
        // 指定Excel文件路径
        String filePath = "D:\\lrs\\Desktop\\单词随记.xlsx";
        // 检查文件是否存在，如果不存在则创建新文件
        File file = new File(filePath);
        Workbook workbook = null;
        if (!file.exists()) {
            try {
                // 创建新的Excel工作簿
                workbook = new XSSFWorkbook();
                String formattedDate = dateFormat();
                // 创建一个空的工作表
                Sheet sheet = workbook.createSheet(formattedDate);
                // 写入数据或设置工作表结构
                // 创建表头
                Row headerRow = sheet.createRow(0);

                // 在前三列分别创建表头
                Cell headerCell1 = headerRow.createCell(0);
                headerCell1.setCellValue("汉字");

                Cell headerCell2 = headerRow.createCell(1);
                headerCell2.setCellValue("假名");

                Cell headerCell3 = headerRow.createCell(2);
                headerCell3.setCellValue("释义");

                System.out.println("Excel文件已创建成功：" + filePath);

                // 定位到最新的空行
                int rowNum = workbook.getSheet(formattedDate).getLastRowNum() + 1;

                List<ExcelWord> excelWordsList = excelWords.getExcelWords();
                if (excelWordsList != null) {
                    for (ExcelWord excelWord : excelWordsList) {
                        Row row = workbook.getSheet(formattedDate).createRow(rowNum++);
                        writeExcelWord(excelWord, row);
                    }
                }
                // 写入Excel文件
                try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                    workbook.write(outputStream);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                System.out.println("Excel文件已存在：" + filePath);
                // 如果文件已存在，读取现有的工作簿
                FileInputStream inputStream = new FileInputStream(file);
                workbook = new XSSFWorkbook(inputStream);

                // 判断工作表是否存在
                String formattedDate = dateFormat();
                if (workbook.getSheetIndex(formattedDate) == -1) {
                    // 不存在
                    System.out.println("创建新的工作表：" + formattedDate);

                    // 创建一个空的工作表
                    Sheet sheet = workbook.createSheet(formattedDate);
                    // 创建表头
                    Row headerRow = sheet.createRow(0);

                    // 在前三列分别创建表头
                    Cell headerCell1 = headerRow.createCell(0);
                    headerCell1.setCellValue("汉字");

                    Cell headerCell2 = headerRow.createCell(1);
                    headerCell2.setCellValue("假名");

                    Cell headerCell3 = headerRow.createCell(2);
                    headerCell3.setCellValue("释义");

                    System.out.println("表头已创建");
                } else {
                    System.out.println("工作表已存在：" + formattedDate);
                }

                // 定位到最新的空行
                int rowNum = workbook.getSheet(formattedDate).getLastRowNum() + 1;

                List<ExcelWord> excelWordsList = excelWords.getExcelWords();
                if (excelWordsList != null) {
                    for (ExcelWord excelWord : excelWordsList) {
                        Row row = workbook.getSheet(formattedDate).createRow(rowNum++);
                        writeExcelWord(excelWord, row);
                    }
                }
                // 写入Excel文件
                try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                    workbook.write(outputStream);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (workbook != null) {
                        workbook.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void writeExcelWord(ExcelWord excelWord, Row row) {
        int cellNum = 0;
        Cell cell = row.createCell(cellNum++);
        cell.setCellValue(excelWord.getWord());

        cell = row.createCell(cellNum++);
        cell.setCellValue(excelWord.getPronounces().toString());

        List<String> meanings = excelWord.getMeanings();
        StringBuilder meaningBuilder = new StringBuilder();
        for (String meaning : meanings) {
            meaningBuilder.append(meaning).append("\n");
        }

        cell = row.createCell(cellNum);
        cell.setCellValue(meaningBuilder.toString());
    }

    /**
     * 收集数据
     *
     * @param response
     * @return
     */
    private ExcelWordResponse getExcelWord(String response) {
        ExcelWordResponse excelWordResponse = new ExcelWordResponse();
        List<ExcelWord> excelWordList = new ArrayList<>();

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
                    String japanesePronunciation = Objects.requireNonNull(pronouncesElement.select("span").first()).text();
                    if (StringUtils.hasText(japanesePronunciation)) {
                        excelWord.setPronounces(japanesePronunciation);
                    }
                    System.out.println("日文发音: " + japanesePronunciation);
                } else {
                    System.out.println("未找到日文发音");
                }
            } else {
                System.out.println("未找到翻译内容");
            }

            // 提取中文注释
            // 查找class为simple的元素
            Element simpleElement = doc.selectFirst("div.simple");

            if (simpleElement != null) {
                // 查找所有的<li>元素并提取内容
                Elements liElements = simpleElement.select("li");
                for (Element liElement : liElements) {
                    // 获取li元素的文本内容，并去除首尾空格
                    String liContent = liElement.text().trim();
                    meanings.add(liContent);
                    System.out.println(liContent);
                }
                excelWord.setMeanings(meanings);
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
                    String pronunciation = spanElement != null ? spanElement.text() : "";
                    excelWord.setPronounces(pronunciation);

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
                        excelWord.setMeanings(meanings);
                    }
                    excelWordList.add(excelWord);
                }
            }
        }
        excelWordResponse.setExcelWords(excelWordList);
        return excelWordResponse;
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
            con.setRequestProperty("Cookie", "HJ_SID=f6k60i-8001-41f8-8d88-70f590d7dbc7; HJ_SSID_3=f6k60i-853b-4733-93f1-3239f49e930c; HJ_UID=eddb6df5-9a54-ad42-45f9-2392b35556d1; HJ_CST=0; HJ_CSST_3=1; TRACKSITEMAP=3; _SREF_3=https%3A%2F%2Fdict.hjenglish.com%2Fjp%2Fjc%2F%E6%95%99%E8%82%B2; _REF=https%3A%2F%2Fdict.hjenglish.com%2Fjp%2Fjc%2F%E6%95%99%E8%82%B2; _SREG_3=dict.hjenglish.com%7C%7Cxiaodi_site%7Cdomain; _REG=dict.hjenglish.com%7C%7Cxiaodi_site%7Cdomain");

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
